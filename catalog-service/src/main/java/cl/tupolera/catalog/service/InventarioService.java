package cl.tupolera.catalog.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import cl.tupolera.catalog.dto.ConsultaInventarioRequest;
import cl.tupolera.catalog.dto.ConsultaInventarioResponse;
import cl.tupolera.catalog.dto.DescuentoRequest;
import cl.tupolera.catalog.dto.ErrorItemInventario;
import cl.tupolera.catalog.dto.ItemInventarioRequest;
import cl.tupolera.catalog.dto.ItemResultadoResponse;
import cl.tupolera.catalog.dto.ProductoInventarioResponse;
import cl.tupolera.catalog.dto.ReposicionRequest;
import cl.tupolera.catalog.dto.ResultadoInventarioResponse;
import cl.tupolera.catalog.dto.VarianteInventarioResponse;
import cl.tupolera.catalog.exception.ApiException;
import cl.tupolera.catalog.exception.InventarioException;
import cl.tupolera.catalog.model.DetalleMovimiento;
import cl.tupolera.catalog.model.MovimientoInventario;
import cl.tupolera.catalog.model.Producto;
import cl.tupolera.catalog.model.ProductoVariante;
import cl.tupolera.catalog.model.TipoMovimiento;
import cl.tupolera.catalog.repository.MovimientoInventarioRepository;
import cl.tupolera.catalog.repository.ProductoRepository;
import cl.tupolera.catalog.repository.ProductoVarianteRepository;

/**
 * Operaciones internas de inventario usadas por sales-service.
 *
 * Garantías:
 *  - ATOMICIDAD: cada operación se ejecuta en UNA transacción; si falla cualquier ítem no queda ninguno aplicado.
 *  - SIN CARRERAS: el descuento es un UPDATE condicional (stock >= cantidad) y las filas se actualizan siempre
 *    en orden de id, para evitar deadlocks entre solicitudes simultáneas.
 *  - IDEMPOTENCIA: cada operación exitosa se registra con una clave única (tipo, clave). Repetir la misma clave
 *    devuelve el resultado original sin tocar el stock; reutilizarla con otro contenido es un conflicto (409).
 *  - REVERSIÓN: devuelve exactamente lo descontado por la operación original, una sola vez.
 */
@Service
public class InventarioService {

    /** Stock máximo por variante en una reposición (evita desbordar el entero). */
    public static final int STOCK_MAXIMO = 1_000_000;

    private static final int MAX_INTENTOS = 3;
    private static final Pattern CLAVE_VALIDA = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
    private static final String HUELLA_REVERSION = sha256Hex("reversion");

    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TransactionTemplate transacciones;

    public InventarioService(ProductoRepository productoRepository, ProductoVarianteRepository varianteRepository,
            MovimientoInventarioRepository movimientoRepository, PlatformTransactionManager transactionManager) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.movimientoRepository = movimientoRepository;
        this.transacciones = new TransactionTemplate(transactionManager);
    }

    private record ItemResuelto(ItemInventarioRequest item, Producto producto, ProductoVariante variante) {
    }

    /* ================================================================== */
    /* Consulta                                                            */
    /* ================================================================== */

    /** Datos vigentes de varios productos y sus variantes. El precio sale siempre del catálogo. */
    @Transactional(readOnly = true)
    public ConsultaInventarioResponse consultar(ConsultaInventarioRequest request) {
        List<Long> ids = request.productoIds().stream().distinct().toList();

        Map<Long, Producto> porId = new HashMap<>();
        productoRepository.findAllById(ids).forEach(p -> porId.put(p.getId(), p));

        List<ProductoInventarioResponse> productos = new ArrayList<>();
        List<Long> noEncontrados = new ArrayList<>();

        for (Long id : ids) {
            Producto p = porId.get(id);
            if (p == null) {
                noEncontrados.add(id);
                continue;
            }

            List<VarianteInventarioResponse> variantes = p.getVariantes().stream()
                    .map(v -> new VarianteInventarioResponse(v.getId(), v.getTalla(), v.getStock()))
                    .toList();

            productos.add(new ProductoInventarioResponse(p.getId(), p.getCodigo(), p.getNombre(), p.isActivo(),
                    p.getPrecio(), p.getDescuento(),
                    // Misma fórmula que usa el catálogo (y el frontend): no se recalcula de otra forma.
                    ProductoMapper.precioFinal(p.getPrecio(), p.getDescuento()),
                    p.getCategoria().getId(), variantes));
        }

        return new ConsultaInventarioResponse(productos, noEncontrados);
    }

    /* ================================================================== */
    /* Descuento                                                           */
    /* ================================================================== */

    public ResultadoInventarioResponse descontar(DescuentoRequest request) {
        validarClave(request.referencia());
        List<ItemInventarioRequest> items = normalizar(request.items());
        String huella = calcularHuella(items);
        String referencia = request.referencia();

        return conReintentos(() -> {
            try {
                return transacciones.execute(status -> descontarEnTransaccion(referencia, items, huella));
            } catch (DataIntegrityViolationException e) {
                // Otra solicitud con la misma referencia se aplicó primero: se devuelve su resultado.
                return reproducirTrasConflicto(TipoMovimiento.DESCUENTO, referencia, huella, e);
            }
        });
    }

    private ResultadoInventarioResponse descontarEnTransaccion(String referencia, List<ItemInventarioRequest> items,
            String huella) {

        Optional<MovimientoInventario> previo = movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO,
                referencia);
        if (previo.isPresent()) {
            return reproducir(previo.get(), huella);
        }

        // Valida todo y junta TODOS los problemas antes de modificar nada.
        List<ItemResuelto> resueltos = resolver(items, true, true);

        // Reclama la clave: si otra solicitud igual llega a la vez, la restricción única la hace fallar.
        MovimientoInventario movimiento = new MovimientoInventario(TipoMovimiento.DESCUENTO, referencia, huella);
        movimientoRepository.saveAndFlush(movimiento);

        // Orden fijo por id de variante: evita deadlocks entre solicitudes que tocan las mismas filas.
        resueltos.sort(Comparator.comparing(r -> r.variante().getId()));

        for (ItemResuelto r : resueltos) {
            int filas = varianteRepository.descontar(r.variante().getId(), r.item().cantidad());

            if (filas == 0) {
                // Otra compra se llevó el stock entre la validación y este UPDATE: se revierte TODO.
                int disponible = leerStocks(List.of(r.variante().getId())).getOrDefault(r.variante().getId(), 0);
                throw new InventarioException(HttpStatus.CONFLICT, "No hay stock suficiente para completar el descuento.",
                        List.of(error(r.item(), "STOCK_INSUFICIENTE",
                                "Stock insuficiente para el producto " + r.item().productoId() + " talla "
                                        + r.item().talla() + ": disponible " + disponible + ".",
                                disponible)));
            }
        }

        registrarDetalles(movimiento, resueltos);
        return resultado(movimiento, false);
    }

    /* ================================================================== */
    /* Reposición                                                          */
    /* ================================================================== */

    public ResultadoInventarioResponse reponer(ReposicionRequest request) {
        validarClave(request.claveIdempotencia());
        List<ItemInventarioRequest> items = normalizar(request.items());
        String huella = calcularHuella(items);
        String clave = request.claveIdempotencia();

        return conReintentos(() -> {
            try {
                return transacciones.execute(status -> reponerEnTransaccion(clave, items, huella));
            } catch (DataIntegrityViolationException e) {
                return reproducirTrasConflicto(TipoMovimiento.REPOSICION, clave, huella, e);
            }
        });
    }

    private ResultadoInventarioResponse reponerEnTransaccion(String clave, List<ItemInventarioRequest> items,
            String huella) {

        Optional<MovimientoInventario> previo = movimientoRepository.findByTipoAndClave(TipoMovimiento.REPOSICION,
                clave);
        if (previo.isPresent()) {
            return reproducir(previo.get(), huella);
        }

        // Se puede reponer stock de productos inactivos; el producto y la talla deben existir.
        List<ItemResuelto> resueltos = resolver(items, false, false);

        MovimientoInventario movimiento = new MovimientoInventario(TipoMovimiento.REPOSICION, clave, huella);
        movimientoRepository.saveAndFlush(movimiento);

        resueltos.sort(Comparator.comparing(r -> r.variante().getId()));

        for (ItemResuelto r : resueltos) {
            int filas = varianteRepository.reponer(r.variante().getId(), r.item().cantidad(),
                    STOCK_MAXIMO - r.item().cantidad());

            if (filas == 0) {
                throw new InventarioException(HttpStatus.CONFLICT, "La reposición supera el stock máximo permitido.",
                        List.of(error(r.item(), "LIMITE_STOCK",
                                "El stock del producto " + r.item().productoId() + " talla " + r.item().talla()
                                        + " no puede superar " + STOCK_MAXIMO + " unidades.",
                                null)));
            }
        }

        registrarDetalles(movimiento, resueltos);
        return resultado(movimiento, false);
    }

    /* ================================================================== */
    /* Reversión (compensación)                                            */
    /* ================================================================== */

    /**
     * Devuelve exactamente las cantidades registradas en el descuento original identificado por "referencia".
     * Se ejecuta una sola vez: repetirla (o hacerlo en paralelo) devuelve el mismo resultado sin volver a sumar.
     */
    public ResultadoInventarioResponse revertirDescuento(String referencia) {
        validarClave(referencia);

        return conReintentos(() -> {
            try {
                return transacciones.execute(status -> revertirEnTransaccion(referencia));
            } catch (DataIntegrityViolationException e) {
                return reproducirTrasConflicto(TipoMovimiento.REVERSION, referencia, HUELLA_REVERSION, e);
            }
        });
    }

    private ResultadoInventarioResponse revertirEnTransaccion(String referencia) {
        MovimientoInventario descuento = movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, referencia)
                .orElseThrow(() -> new InventarioException(HttpStatus.NOT_FOUND,
                        "No existe un descuento aplicado con esa referencia.",
                        List.of(new ErrorItemInventario(null, null, "DESCUENTO_NO_ENCONTRADO",
                                "No hay un descuento registrado con la referencia indicada.", null))));

        Optional<MovimientoInventario> previa = movimientoRepository.findByTipoAndClave(TipoMovimiento.REVERSION,
                referencia);
        if (previa.isPresent()) {
            return reproducir(previa.get(), HUELLA_REVERSION);
        }

        // Reclama la clave de reversión: de varias solicitudes simultáneas, solo una continúa.
        MovimientoInventario reversion = new MovimientoInventario(TipoMovimiento.REVERSION, referencia,
                HUELLA_REVERSION);
        movimientoRepository.saveAndFlush(reversion);

        // Las cantidades salen del registro del descuento original, nunca de la solicitud.
        List<DetalleMovimiento> originales = new ArrayList<>(descuento.getDetalles());
        originales.sort(Comparator.comparing(DetalleMovimiento::getVarianteId));

        for (DetalleMovimiento d : originales) {
            int filas = varianteRepository.reponer(d.getVarianteId(), d.getCantidad(),
                    Integer.MAX_VALUE - d.getCantidad());

            if (filas == 0) {
                throw new InventarioException(HttpStatus.CONFLICT, "No se pudo revertir el descuento.",
                        List.of(new ErrorItemInventario(d.getProductoId(), d.getTalla(), "VARIANTE_NO_ENCONTRADA",
                                "La variante " + d.getVarianteId() + " ya no existe: no se puede devolver su stock.",
                                null)));
            }
        }

        Map<Long, Integer> stocks = leerStocks(originales.stream().map(DetalleMovimiento::getVarianteId).toList());
        for (DetalleMovimiento d : originales) {
            reversion.getDetalles().add(new DetalleMovimiento(d.getProductoId(), d.getVarianteId(), d.getTalla(),
                    d.getCantidad(), stocks.getOrDefault(d.getVarianteId(), 0)));
        }

        return resultado(reversion, false);
    }

    /* ================================================================== */
    /* Piezas comunes                                                      */
    /* ================================================================== */

    /** Valida productos y variantes y junta todos los errores; si hay alguno lanza una sola excepción. */
    private List<ItemResuelto> resolver(List<ItemInventarioRequest> items, boolean exigirActivo,
            boolean validarStock) {

        List<Long> ids = items.stream().map(ItemInventarioRequest::productoId).distinct().toList();
        Map<Long, Producto> productos = new HashMap<>();
        productoRepository.findAllById(ids).forEach(p -> productos.put(p.getId(), p));

        List<ErrorItemInventario> errores = new ArrayList<>();
        List<ItemResuelto> resueltos = new ArrayList<>();

        for (ItemInventarioRequest item : items) {
            Producto producto = productos.get(item.productoId());

            if (producto == null) {
                errores.add(error(item, "PRODUCTO_NO_ENCONTRADO",
                        "El producto " + item.productoId() + " no existe.", null));
                continue;
            }

            if (exigirActivo && !producto.isActivo()) {
                errores.add(error(item, "PRODUCTO_INACTIVO",
                        "El producto " + item.productoId() + " (" + producto.getCodigo() + ") está inactivo.", null));
                continue;
            }

            ProductoVariante variante = producto.getVariantes().stream()
                    .filter(v -> v.getTalla().equals(item.talla()))
                    .findFirst()
                    .orElse(null);

            if (variante == null) {
                errores.add(error(item, "VARIANTE_NO_ENCONTRADA",
                        "El producto " + item.productoId() + " no tiene la talla " + item.talla() + ".", null));
                continue;
            }

            if (validarStock && variante.getStock() < item.cantidad()) {
                errores.add(error(item, "STOCK_INSUFICIENTE",
                        "Stock insuficiente para el producto " + item.productoId() + " talla " + item.talla()
                                + ": pediste " + item.cantidad() + " y hay " + variante.getStock() + ".",
                        variante.getStock()));
                continue;
            }

            resueltos.add(new ItemResuelto(item, producto, variante));
        }

        if (!errores.isEmpty()) {
            throw new InventarioException(estadoPara(errores), "La operación no se pudo aplicar: " + errores.size()
                    + " ítem(s) con problemas. No se modificó ningún stock.", errores);
        }

        return resueltos;
    }

    /** 404 si algún producto o talla no existe; 409 si hay productos inactivos o stock insuficiente. */
    private static HttpStatus estadoPara(List<ErrorItemInventario> errores) {
        boolean noExiste = errores.stream().anyMatch(e ->
                "PRODUCTO_NO_ENCONTRADO".equals(e.codigo()) || "VARIANTE_NO_ENCONTRADA".equals(e.codigo()));
        return noExiste ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
    }

    private void registrarDetalles(MovimientoInventario movimiento, List<ItemResuelto> resueltos) {
        Map<Long, Integer> stocks = leerStocks(resueltos.stream().map(r -> r.variante().getId()).toList());

        for (ItemResuelto r : resueltos) {
            movimiento.getDetalles().add(new DetalleMovimiento(r.producto().getId(), r.variante().getId(),
                    r.item().talla(), r.item().cantidad(), stocks.getOrDefault(r.variante().getId(), 0)));
        }
    }

    private Map<Long, Integer> leerStocks(List<Long> varianteIds) {
        Map<Long, Integer> stocks = new HashMap<>();
        for (Object[] fila : varianteRepository.stocksPorIds(varianteIds)) {
            stocks.put(((Number) fila[0]).longValue(), ((Number) fila[1]).intValue());
        }
        return stocks;
    }

    /** Resultado guardado de una operación ya aplicada; si la clave se reutilizó con otro contenido, 409. */
    private ResultadoInventarioResponse reproducir(MovimientoInventario previo, String huella) {
        if (!previo.getHuella().equals(huella)) {
            throw new InventarioException(HttpStatus.CONFLICT,
                    "La clave ya se usó con un contenido distinto. Usa una clave nueva para una operación nueva.",
                    List.of(new ErrorItemInventario(null, null, "CONFLICTO_IDEMPOTENCIA",
                            "La referencia " + previo.getClave() + " ya fue aplicada con ítems diferentes.", null)));
        }

        // Un descuento revertido no puede volver a "reproducirse" como si siguiera vigente.
        if (previo.getTipo() == TipoMovimiento.DESCUENTO
                && movimientoRepository.existsByTipoAndClave(TipoMovimiento.REVERSION, previo.getClave())) {
            throw new InventarioException(HttpStatus.CONFLICT, "El descuento de esa referencia ya fue revertido.",
                    List.of(new ErrorItemInventario(null, null, "REFERENCIA_REVERTIDA",
                            "La referencia " + previo.getClave() + " ya fue revertida; usa una referencia nueva.",
                            null)));
        }

        return resultado(previo, true);
    }

    /** Se llama cuando otra solicitud simultánea con la misma clave ganó la restricción única. */
    private ResultadoInventarioResponse reproducirTrasConflicto(TipoMovimiento tipo, String clave, String huella,
            DataIntegrityViolationException original) {

        return transacciones.execute(status -> movimientoRepository.findByTipoAndClave(tipo, clave)
                .map(previo -> reproducir(previo, huella))
                // Si no existe, el error no era un duplicado: se propaga tal cual.
                .orElseThrow(() -> original));
    }

    private static ResultadoInventarioResponse resultado(MovimientoInventario movimiento, boolean repetida) {
        List<ItemResultadoResponse> items = movimiento.getDetalles().stream()
                .map(d -> new ItemResultadoResponse(d.getProductoId(), d.getVarianteId(), d.getTalla(),
                        d.getCantidad(), d.getStockDespues()))
                .toList();

        return new ResultadoInventarioResponse(movimiento.getClave(), movimiento.getTipo().name(), repetida, items);
    }

    private static ErrorItemInventario error(ItemInventarioRequest item, String codigo, String mensaje,
            Integer disponible) {
        return new ErrorItemInventario(item.productoId(), item.talla(), codigo, mensaje, disponible);
    }

    /** Reintenta (hasta 3 veces) si MySQL aborta la transacción por un bloqueo cruzado (deadlock). */
    private <T> T conReintentos(Supplier<T> operacion) {
        int intento = 1;
        while (true) {
            try {
                return operacion.get();
            } catch (PessimisticLockingFailureException e) {
                if (intento >= MAX_INTENTOS) {
                    throw new InventarioException(HttpStatus.SERVICE_UNAVAILABLE,
                            "El inventario está ocupado. Reintenta en unos segundos.", List.of());
                }
                try {
                    Thread.sleep(25L * intento);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new InventarioException(HttpStatus.SERVICE_UNAVAILABLE,
                            "La operación fue interrumpida.", List.of());
                }
                intento++;
            }
        }
    }

    /** Agrupa y limpia los ítems; un mismo producto+talla no puede repetirse en la solicitud. */
    static List<ItemInventarioRequest> normalizar(List<ItemInventarioRequest> items) {
        Set<String> vistos = new HashSet<>();
        List<ItemInventarioRequest> resultado = new ArrayList<>();

        for (ItemInventarioRequest item : items) {
            String talla = item.talla().trim();

            if (!vistos.add(item.productoId() + "|" + talla)) {
                throw ApiException.badRequest("El producto " + item.productoId() + " talla " + talla
                        + " está repetido en la solicitud. Suma las cantidades en un solo ítem.");
            }
            resultado.add(new ItemInventarioRequest(item.productoId(), talla, item.cantidad()));
        }
        return resultado;
    }

    static void validarClave(String clave) {
        if (clave == null || !CLAVE_VALIDA.matcher(clave).matches()) {
            throw ApiException.badRequest("La referencia debe tener entre 8 y 64 caracteres "
                    + "(letras, números, punto, guion o guion bajo); se recomienda un UUID.");
        }
    }

    /** SHA-256 de los ítems ordenados: identifica el contenido de una solicitud. */
    public static String calcularHuella(List<ItemInventarioRequest> items) {
        String canonico = items.stream()
                .sorted(Comparator.comparing(ItemInventarioRequest::productoId)
                        .thenComparing(ItemInventarioRequest::talla))
                .map(i -> i.productoId() + "|" + i.talla().trim() + "|" + i.cantidad())
                .collect(Collectors.joining(";"));
        return sha256Hex(canonico);
    }

    private static String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
