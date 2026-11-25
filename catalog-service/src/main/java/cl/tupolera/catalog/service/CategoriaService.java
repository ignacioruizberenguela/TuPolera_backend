package cl.tupolera.catalog.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.catalog.dto.AtributoDto;
import cl.tupolera.catalog.dto.CategoriaRequest;
import cl.tupolera.catalog.dto.CategoriaResponse;
import cl.tupolera.catalog.dto.MedidaDto;
import cl.tupolera.catalog.exception.ApiException;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.model.CategoriaAtributo;
import cl.tupolera.catalog.model.CategoriaMedida;
import cl.tupolera.catalog.repository.CategoriaRepository;
import cl.tupolera.catalog.repository.ProductoRepository;
import cl.tupolera.catalog.util.Texto;

/** Reglas de negocio de las categorías configurables (tallas, atributos y medidas). */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper mapper;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
            CategoriaMapper mapper) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(boolean incluirInactivas) {
        List<Categoria> categorias = incluirInactivas
                ? categoriaRepository.findAllByOrderByIdAsc()
                : categoriaRepository.findByActivaTrueOrderByIdAsc();

        return categorias.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id, boolean incluirInactivas) {
        Categoria categoria = buscar(id);

        // Para quien no puede ver inactivos, una categoría desactivada "no existe".
        if (!categoria.isActiva() && !incluirInactivas) {
            throw ApiException.notFound("La categoría no existe.");
        }
        return mapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw ApiException.conflict("Ya existe una categoría con ese nombre.");
        }

        Categoria categoria = new Categoria();
        aplicar(categoria, request, nombre);
        categoria.setActiva(request.activa() == null || request.activa());

        return mapper.toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw ApiException.conflict("Ya existe una categoría con ese nombre.");
        }

        boolean desactiva = request.activa() != null && !request.activa();
        if (desactiva && categoria.isActiva() && productoRepository.countByCategoriaIdAndActivoTrue(id) > 0) {
            throw ApiException.conflict("No puedes desactivar la categoría porque tiene productos activos.");
        }

        aplicar(categoria, request, nombre);
        if (request.activa() != null) {
            categoria.setActiva(request.activa());
        }

        return mapper.toResponse(categoriaRepository.save(categoria));
    }

    /**
     * Elimina o desactiva según sus relaciones con productos:
     *  - con productos activos: 409 (primero hay que mover o desactivar esos productos);
     *  - solo con productos inactivos: se desactiva (los productos históricos conservan su categoría);
     *  - sin productos: se elimina físicamente.
     */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);

        if (productoRepository.countByCategoriaIdAndActivoTrue(id) > 0) {
            throw ApiException.conflict("No puedes eliminar la categoría porque tiene productos activos.");
        }

        if (productoRepository.countByCategoriaId(id) > 0) {
            categoria.setActiva(false);
            categoriaRepository.save(categoria);
        } else {
            categoriaRepository.delete(categoria);
        }
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La categoría no existe."));
    }

    /* ------------------------------------------------------------------ */

    private void aplicar(Categoria categoria, CategoriaRequest request, String nombre) {
        categoria.setNombre(nombre);
        categoria.setDescripcion(Texto.limpiar(request.descripcion()));
        categoria.setImagen(Texto.limpiar(request.imagen()));

        // Tallas: sin duplicados, conservando el orden indicado.
        Set<String> tallas = new java.util.LinkedHashSet<>();
        for (String talla : request.tallas()) {
            String limpia = talla.trim();
            if (!tallas.add(limpia)) {
                throw ApiException.badRequest("La talla \"" + limpia + "\" está repetida.");
            }
        }
        categoria.getTallas().clear();
        categoria.getTallas().addAll(tallas);

        sincronizarAtributos(categoria, request.atributos() == null ? List.of() : request.atributos());
        sincronizarMedidas(categoria, request.medidas() == null ? List.of() : request.medidas());
    }

    /**
     * Actualiza los atributos "en el lugar" (por clave) en vez de borrarlos y recrearlos,
     * para no violar la restricción única (categoria_id, clave) al guardar.
     */
    private void sincronizarAtributos(Categoria categoria, List<AtributoDto> dtos) {
        Map<String, AtributoDto> pedidos = new LinkedHashMap<>();
        Set<String> usadas = new HashSet<>();
        for (AtributoDto dto : dtos) {
            if (dto.id() != null && !dto.id().isBlank()) {
                usadas.add(dto.id());
            }
        }

        Set<String> nombres = new HashSet<>();
        for (AtributoDto dto : dtos) {
            String nombre = dto.nombre().trim();
            if (!nombres.add(nombre.toLowerCase())) {
                throw ApiException.badRequest("No puede haber dos atributos con el nombre \"" + nombre + "\".");
            }

            String clave = dto.id() != null && !dto.id().isBlank()
                    ? dto.id()
                    : generarClave(nombre, usadas, "atributo");

            if (pedidos.putIfAbsent(clave, dto) != null) {
                throw ApiException.badRequest("El id de atributo \"" + clave + "\" está repetido.");
            }

            validarOpciones(dto, nombre);
        }

        categoria.getAtributos().removeIf(existente -> !pedidos.containsKey(existente.getClave()));

        int orden = 0;
        for (Map.Entry<String, AtributoDto> entrada : pedidos.entrySet()) {
            String clave = entrada.getKey();
            AtributoDto dto = entrada.getValue();

            CategoriaAtributo atributo = categoria.getAtributos().stream()
                    .filter(a -> a.getClave().equals(clave))
                    .findFirst()
                    .orElse(null);

            if (atributo == null) {
                atributo = new CategoriaAtributo(categoria, clave);
                categoria.getAtributos().add(atributo);
            }

            atributo.setNombre(dto.nombre().trim());
            atributo.setTipo(dto.tipo());
            atributo.setObligatorio(dto.obligatorio());
            atributo.setOrden(orden++);

            atributo.getOpciones().clear();
            if (CategoriaAtributo.SELECCION.equals(dto.tipo())) {
                atributo.getOpciones().addAll(opcionesLimpias(dto));
            }
        }
    }

    private void validarOpciones(AtributoDto dto, String nombre) {
        if (CategoriaAtributo.SELECCION.equals(dto.tipo()) && opcionesLimpias(dto).size() < 2) {
            throw ApiException.badRequest(
                    "El atributo \"" + nombre + "\" necesita al menos dos opciones distintas.");
        }
    }

    private static List<String> opcionesLimpias(AtributoDto dto) {
        if (dto.opciones() == null) {
            return List.of();
        }
        return dto.opciones().stream().map(String::trim).filter(o -> !o.isEmpty()).distinct().toList();
    }

    private void sincronizarMedidas(Categoria categoria, List<MedidaDto> dtos) {
        Map<String, MedidaDto> pedidas = new LinkedHashMap<>();
        Set<String> usadas = new HashSet<>();
        for (MedidaDto dto : dtos) {
            if (dto.id() != null && !dto.id().isBlank()) {
                usadas.add(dto.id());
            }
        }

        Set<String> nombres = new HashSet<>();
        for (MedidaDto dto : dtos) {
            String nombre = dto.nombre().trim();
            if (!nombres.add(nombre.toLowerCase())) {
                throw ApiException.badRequest("No puede haber dos medidas con el nombre \"" + nombre + "\".");
            }

            if (!dto.solicitarCliente() && !dto.registrarProducto()) {
                throw ApiException.badRequest(
                        "La medida \"" + nombre + "\" debe solicitarse al cliente o registrarse en el producto.");
            }

            String clave = dto.id() != null && !dto.id().isBlank()
                    ? dto.id()
                    : generarClave(nombre, usadas, "medida");

            if (pedidas.putIfAbsent(clave, dto) != null) {
                throw ApiException.badRequest("El id de medida \"" + clave + "\" está repetido.");
            }
        }

        categoria.getMedidas().removeIf(existente -> !pedidas.containsKey(existente.getClave()));

        int orden = 0;
        for (Map.Entry<String, MedidaDto> entrada : pedidas.entrySet()) {
            String clave = entrada.getKey();
            MedidaDto dto = entrada.getValue();

            CategoriaMedida medida = categoria.getMedidas().stream()
                    .filter(m -> m.getClave().equals(clave))
                    .findFirst()
                    .orElse(null);

            if (medida == null) {
                medida = new CategoriaMedida(categoria, clave);
                categoria.getMedidas().add(medida);
            }

            medida.setNombre(dto.nombre().trim());
            medida.setUnidad(dto.unidad() == null || dto.unidad().isBlank() ? "cm" : dto.unidad().trim());
            medida.setSolicitarCliente(dto.solicitarCliente());
            medida.setRegistrarProducto(dto.registrarProducto());
            medida.setOrden(orden++);
        }
    }

    /** Genera un id único dentro de la categoría a partir del nombre. */
    private static String generarClave(String nombre, Set<String> usadas, String respaldo) {
        String base = Texto.aIdentificador(nombre);
        if (base.isEmpty()) {
            base = respaldo;
        }
        if (base.length() > 55) {
            base = base.substring(0, 55);
        }

        String clave = base;
        int numero = 2;
        while (usadas.contains(clave)) {
            clave = base + "_" + numero;
            numero++;
        }
        usadas.add(clave);
        return clave;
    }
}
