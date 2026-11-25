package cl.tupolera.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.catalog.dto.ProductoRequest;
import cl.tupolera.catalog.dto.ProductoResponse;
import cl.tupolera.catalog.dto.VarianteRequest;
import cl.tupolera.catalog.exception.ApiException;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.model.CategoriaAtributo;
import cl.tupolera.catalog.model.CategoriaMedida;
import cl.tupolera.catalog.model.Producto;
import cl.tupolera.catalog.model.ProductoVariante;
import cl.tupolera.catalog.repository.CategoriaRepository;
import cl.tupolera.catalog.repository.ProductoRepository;
import cl.tupolera.catalog.util.Texto;

/**
 * Reglas de negocio de productos. Los atributos, las tallas y las medidas que se aceptan
 * dependen de la configuración de la categoría del producto.
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper mapper;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository,
            ProductoMapper mapper) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(boolean incluirInactivos) {
        List<Producto> productos = incluirInactivos
                ? productoRepository.findAllByOrderByIdAsc()
                : productoRepository.findByActivoTrueOrderByIdAsc();

        return productos.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorCategoria(Long categoriaId, boolean incluirInactivos) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw ApiException.notFound("La categoría no existe.");
        }

        List<Producto> productos = incluirInactivos
                ? productoRepository.findByCategoriaIdOrderByIdAsc(categoriaId)
                : productoRepository.findByCategoriaIdAndActivoTrueOrderByIdAsc(categoriaId);

        return productos.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id, boolean incluirInactivos) {
        Producto producto = buscar(id);

        // Para quien no puede ver inactivos, un producto desactivado "no existe".
        if (!producto.isActivo() && !incluirInactivos) {
            throw ApiException.notFound("El producto no existe.");
        }
        return mapper.toResponse(producto);
    }

    /**
     * Productos activos con al menos una talla en stock crítico (stock de la talla menor o igual
     * al stock crítico del producto). Incluye el caso en que el stock total llega al umbral.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> criticos() {
        return productoRepository.findByActivoTrueOrderByIdAsc().stream()
                .filter(producto -> !ProductoMapper.tallasCriticas(producto).isEmpty())
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = request.codigo().trim();

        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw ApiException.conflict("Ya existe un producto con ese código.");
        }

        Categoria categoria = buscarCategoriaActiva(request.categoriaId());

        Producto producto = new Producto();
        aplicar(producto, request, codigo, categoria);
        producto.setActivo(request.activo() == null || request.activo());

        return mapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        String codigo = request.codigo().trim();

        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw ApiException.conflict("Ya existe un producto con ese código.");
        }

        Categoria categoria = buscarCategoriaActiva(request.categoriaId());

        aplicar(producto, request, codigo, categoria);
        if (request.activo() != null) {
            producto.setActivo(request.activo());
        }

        return mapper.toResponse(productoRepository.save(producto));
    }

    /**
     * DELETE desactiva el producto (no se borra): puede estar asociado a boletas históricas
     * registradas en sales-service. Se puede reactivar con PUT (activo = true).
     */
    @Transactional
    public void desactivar(Long id) {
        Producto producto = buscar(id);
        producto.setActivo(false);
        producto.setFechaActualizacion(LocalDateTime.now());
        productoRepository.save(producto);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El producto no existe."));
    }

    private Categoria buscarCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> ApiException.badRequest("La categoría indicada no existe."));

        if (!categoria.isActiva()) {
            throw ApiException.badRequest("La categoría indicada está desactivada.");
        }
        return categoria;
    }

    /* ------------------------------------------------------------------ */

    private void aplicar(Producto producto, ProductoRequest request, String codigo, Categoria categoria) {
        // Se validan primero los datos dinámicos: si hay un error no se modifica nada.
        Map<String, String> atributos = validarAtributos(categoria, request.atributos());

        producto.setCodigo(codigo);
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(Texto.limpiar(request.descripcion()));
        producto.setPrecio(request.precio());
        producto.setDescuento(request.descuento() == null ? 0 : request.descuento());
        producto.setColor(Texto.limpiar(request.color()));
        producto.setCategoria(categoria);
        producto.setImagen(Texto.limpiar(request.imagen()));
        producto.setStockCritico(
                request.stockCritico() == null ? Producto.STOCK_CRITICO_POR_DEFECTO : request.stockCritico());
        producto.setFechaActualizacion(LocalDateTime.now());

        producto.getAtributos().clear();
        producto.getAtributos().putAll(atributos);

        sincronizarVariantes(producto, categoria, request.tallas());
    }

    /** Valida los atributos según el tipo, las opciones y la obligatoriedad definidos en la categoría. */
    private Map<String, String> validarAtributos(Categoria categoria, Map<String, Object> recibidos) {
        Map<String, Object> valores = recibidos == null ? Map.of() : recibidos;
        Map<String, String> resultado = new LinkedHashMap<>();

        for (CategoriaAtributo atributo : categoria.getAtributos()) {
            Object crudo = valores.get(atributo.getClave());
            String nombre = atributo.getNombre();

            if (CategoriaAtributo.BOOLEANO.equals(atributo.getTipo())) {
                resultado.put(atributo.getClave(), String.valueOf(aBooleano(crudo, nombre)));
                continue;
            }

            String texto = crudo == null ? "" : String.valueOf(crudo).trim();

            if (texto.isEmpty()) {
                if (atributo.isObligatorio()) {
                    throw ApiException.badRequest("Completa el atributo \"" + nombre + "\".");
                }
                continue;
            }

            if (CategoriaAtributo.NUMERO.equals(atributo.getTipo())) {
                try {
                    texto = new BigDecimal(texto).stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    throw ApiException.badRequest("El atributo \"" + nombre + "\" debe ser un número.");
                }
            } else if (CategoriaAtributo.SELECCION.equals(atributo.getTipo())
                    && !atributo.getOpciones().contains(texto)) {
                throw ApiException.badRequest("El valor de \"" + nombre + "\" debe ser una de las opciones: "
                        + String.join(", ", atributo.getOpciones()) + ".");
            } else if (texto.length() > 255) {
                throw ApiException.badRequest("El atributo \"" + nombre + "\" es demasiado largo.");
            }

            resultado.put(atributo.getClave(), texto);
        }

        // Las claves que no pertenecen a la categoría se ignoran.
        return resultado;
    }

    private static boolean aBooleano(Object valor, String nombre) {
        if (valor == null) {
            return false;
        }
        if (valor instanceof Boolean booleano) {
            return booleano;
        }
        String texto = String.valueOf(valor).trim().toLowerCase();
        if (texto.equals("true") || texto.equals("false")) {
            return Boolean.parseBoolean(texto);
        }
        throw ApiException.badRequest("El atributo \"" + nombre + "\" debe ser verdadero o falso.");
    }

    /**
     * Actualiza las variantes "en el lugar" (por talla): conserva las existentes, agrega las nuevas y
     * elimina las que ya no vienen. Evita violar la restricción única (producto_id, talla).
     */
    private void sincronizarVariantes(Producto producto, Categoria categoria, Map<String, VarianteRequest> pedidas) {
        // Validación previa de todas las tallas y medidas.
        Map<String, Map<String, BigDecimal>> medidasValidadas = new LinkedHashMap<>();

        for (Map.Entry<String, VarianteRequest> entrada : pedidas.entrySet()) {
            String talla = entrada.getKey().trim();

            if (!categoria.getTallas().contains(talla)) {
                throw ApiException.badRequest("La talla \"" + talla + "\" no pertenece a la categoría "
                        + categoria.getNombre() + ".");
            }
            if (medidasValidadas.containsKey(talla)) {
                throw ApiException.badRequest("La talla \"" + talla + "\" está repetida.");
            }

            medidasValidadas.put(talla, validarMedidas(categoria, talla, entrada.getValue().medidas()));
        }

        producto.getVariantes().removeIf(v -> !medidasValidadas.containsKey(v.getTalla()));

        for (Map.Entry<String, VarianteRequest> entrada : pedidas.entrySet()) {
            String talla = entrada.getKey().trim();

            ProductoVariante variante = producto.getVariantes().stream()
                    .filter(v -> v.getTalla().equals(talla))
                    .findFirst()
                    .orElse(null);

            if (variante == null) {
                variante = new ProductoVariante(producto, talla);
                producto.getVariantes().add(variante);
            }

            variante.setStock(entrada.getValue().stock());
            variante.getMedidas().clear();
            variante.getMedidas().putAll(medidasValidadas.get(talla));
        }
    }

    /** Exige las medidas que la categoría marca como "registrar en el producto" (mayores que cero). */
    private Map<String, BigDecimal> validarMedidas(Categoria categoria, String talla,
            Map<String, BigDecimal> recibidas) {
        Map<String, BigDecimal> valores = recibidas == null ? Map.of() : recibidas;
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();

        for (CategoriaMedida medida : categoria.getMedidas()) {
            if (!medida.isRegistrarProducto()) {
                continue;
            }

            BigDecimal valor = valores.get(medida.getClave());

            if (valor == null || valor.signum() <= 0) {
                throw ApiException.badRequest("Ingresa \"" + medida.getNombre() + "\" (" + medida.getUnidad()
                        + ") para la talla " + talla + ", con un valor mayor que cero.");
            }
            if (valor.scale() > 2 || valor.precision() - valor.scale() > 6) {
                throw ApiException.badRequest("La medida \"" + medida.getNombre()
                        + "\" admite hasta 2 decimales y 6 enteros.");
            }

            resultado.put(medida.getClave(), valor);
        }
        return resultado;
    }
}
