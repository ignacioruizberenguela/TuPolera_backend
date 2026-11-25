package cl.tupolera.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.tupolera.catalog.dto.ProductoRequest;
import cl.tupolera.catalog.dto.ProductoResponse;
import cl.tupolera.catalog.dto.VarianteRequest;
import cl.tupolera.catalog.exception.ApiException;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.model.Producto;
import cl.tupolera.catalog.repository.CategoriaRepository;
import cl.tupolera.catalog.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private CategoriaRepository categoriaRepository;

    private ProductoService service;
    private Categoria poleras;

    @BeforeEach
    void preparar() {
        service = new ProductoService(productoRepository, categoriaRepository, new ProductoMapper());
        poleras = TestData.poleras();
    }

    private static int status(Throwable t) {
        return ((ApiException) t).getStatus().value();
    }

    private void categoriaExiste() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
    }

    /* ------------------------- creación ------------------------- */

    @Test
    void creaUnProductoConVariantesAtributosYValoresPorDefecto() {
        categoriaExiste();
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse respuesta = service.crear(
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Corta")));

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        Producto guardado = captor.getValue();

        assertThat(guardado.getCodigo()).isEqualTo("POL-010");
        assertThat(guardado.getNombre()).isEqualTo("Urban Red");
        assertThat(guardado.getDescuento()).isZero();
        assertThat(guardado.getStockCritico()).isEqualTo(5);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getAtributos()).containsEntry("tipo_manga", "Corta");
        assertThat(guardado.getVariantes()).hasSize(2);

        // El stock general es la suma de las variantes y cada talla conserva el suyo.
        assertThat(respuesta.stock()).isEqualTo(15);
        assertThat(respuesta.tallas().get("S").stock()).isEqualTo(5);
        assertThat(respuesta.tallas().get("M").medidas()).containsEntry("contorno_pecho", BigDecimal.valueOf(102.0));
        assertThat(respuesta.atributos()).containsEntry("tipo_manga", "Corta");
        assertThat(respuesta.categoriaId()).isEqualTo(1L);
    }

    @Test
    void calculaElPrecioFinalConDescuento() {
        assertThat(ProductoMapper.precioFinal(12990, 50)).isEqualTo(6495);
        assertThat(ProductoMapper.precioFinal(14990, 10)).isEqualTo(13491);
        assertThat(ProductoMapper.precioFinal(8990, 0)).isEqualTo(8990);
    }

    @Test
    void rechazaUnCodigoDuplicadoConConflicto() {
        when(productoRepository.existsByCodigoIgnoreCase("POL-010")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));

        verify(productoRepository, never()).save(any());
    }

    @Test
    void rechazaUnaCategoriaInexistenteOInactiva() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.crear(
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));

        poleras.setActiva(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
        assertThatThrownBy(() -> service.crear(
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    @Test
    void rechazaUnaTallaQueNoPerteneceALaCategoria() {
        categoriaExiste();
        Map<String, VarianteRequest> tallas = new LinkedHashMap<>();
        tallas.put("XXL", TestData.variante(3, 120, 74));

        assertThatThrownBy(() -> service.crear(TestData.solicitud(tallas, Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(400);
                    assertThat(e.getMessage()).contains("XXL");
                });
    }

    @Test
    void exigeLasMedidasQueLaCategoriaMarcaComoRegistrables() {
        categoriaExiste();
        Map<String, VarianteRequest> tallas = new LinkedHashMap<>();
        tallas.put("S", new VarianteRequest(5, Map.of("contorno_pecho", BigDecimal.valueOf(96))));

        assertThatThrownBy(() -> service.crear(TestData.solicitud(tallas, Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(400);
                    assertThat(e.getMessage()).contains("Largo de prenda");
                });
    }

    @Test
    void rechazaMedidasCeroONegativas() {
        categoriaExiste();
        Map<String, VarianteRequest> tallas = new LinkedHashMap<>();
        tallas.put("S", TestData.variante(5, 0, 66));

        assertThatThrownBy(() -> service.crear(TestData.solicitud(tallas, Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    @Test
    void validaLosAtributosSegunLaCategoria() {
        categoriaExiste();

        // Obligatorio y ausente
        assertThatThrownBy(() -> service.crear(TestData.solicitud(TestData.tallasValidas(), Map.of())))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(400);
                    assertThat(e.getMessage()).contains("Tipo de manga");
                });

        // Opción que no existe
        assertThatThrownBy(() -> service.crear(
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Sin mangas"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    /* ------------------------- actualización ------------------------- */

    @Test
    void actualizaLasVariantesEnElLugarYEliminaLasQueYaNoVienen() {
        categoriaExiste();
        Producto existente = TestData.producto(poleras, 1L, 5, 5, 10, 8);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, VarianteRequest> tallas = new LinkedHashMap<>();
        tallas.put("S", TestData.variante(99, 96, 66));
        tallas.put("M", TestData.variante(10, 102, 68));

        ProductoResponse respuesta = service.actualizar(1L,
                TestData.solicitud(tallas, Map.of("tipo_manga", "Larga")));

        assertThat(existente.getVariantes()).extracting("talla").containsExactly("S", "M");
        assertThat(respuesta.tallas().get("S").stock()).isEqualTo(99);
        assertThat(respuesta.stock()).isEqualTo(109);
    }

    @Test
    void actualizarUnProductoInexistenteResponde404() {
        when(productoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(9L,
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(404));
    }

    @Test
    void actualizarConElCodigoDeOtroProductoResponde409() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(TestData.producto(poleras, 1L, 5, 5)));
        when(productoRepository.existsByCodigoIgnoreCaseAndIdNot("POL-010", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L,
                TestData.solicitud(TestData.tallasValidas(), Map.of("tipo_manga", "Corta"))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));
    }

    /* ------------------------- eliminación y consulta ------------------------- */

    @Test
    void eliminarDesactivaElProductoSinBorrarlo() {
        Producto existente = TestData.producto(poleras, 1L, 5, 5);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.desactivar(1L);

        assertThat(existente.isActivo()).isFalse();
        verify(productoRepository).save(existente);
        verify(productoRepository, never()).delete(any(Producto.class));
    }

    @Test
    void unProductoInactivoNoExisteParaElPublicoPeroSiParaElPersonal() {
        Producto inactivo = TestData.producto(poleras, 1L, 5, 5);
        inactivo.setActivo(false);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> service.obtener(1L, false))
                .satisfies(e -> assertThat(status(e)).isEqualTo(404));

        assertThat(service.obtener(1L, true).activo()).isFalse();
    }

    @Test
    void elListadoPublicoSoloIncluyeProductosActivos() {
        when(productoRepository.findByActivoTrueOrderByIdAsc())
                .thenReturn(List.of(TestData.producto(poleras, 1L, 5, 5)));
        when(productoRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(TestData.producto(poleras, 1L, 5, 5), TestData.producto(poleras, 2L, 5, 5)));

        assertThat(service.listar(false)).hasSize(1);
        assertThat(service.listar(true)).hasSize(2);
    }

    @Test
    void filtraProductosPorCategoria() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);
        when(productoRepository.findByCategoriaIdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(List.of(TestData.producto(poleras, 1L, 5, 5)));

        assertThat(service.listarPorCategoria(1L, false)).hasSize(1);

        when(categoriaRepository.existsById(77L)).thenReturn(false);
        assertThatThrownBy(() -> service.listarPorCategoria(77L, false))
                .satisfies(e -> assertThat(status(e)).isEqualTo(404));
    }

    /* ------------------------- stock crítico ------------------------- */

    @Test
    void detectaLosProductosConTallasEnStockCritico() {
        Producto conStockBajo = TestData.producto(poleras, 1L, 5, 20, 3, 10);   // la talla M tiene 3 (<= 5)
        Producto agotada = TestData.producto(poleras, 2L, 5, 8, 0, 9);          // la talla M tiene 0
        Producto sano = TestData.producto(poleras, 3L, 5, 20, 20, 20);
        Producto umbralPropio = TestData.producto(poleras, 4L, 1, 2, 2, 2);     // umbral 1: ninguna talla lo alcanza

        when(productoRepository.findByActivoTrueOrderByIdAsc())
                .thenReturn(List.of(conStockBajo, agotada, sano, umbralPropio));

        List<ProductoResponse> criticos = service.criticos();

        assertThat(criticos).extracting(ProductoResponse::id).containsExactly(1L, 2L);
        assertThat(criticos.get(0).critico()).isTrue();
        assertThat(criticos.get(0).tallasCriticas()).containsExactly("M");
        assertThat(criticos.get(1).tallasCriticas()).containsExactly("M");
    }

    @Test
    void unProductoCuyoStockTotalLlegaAlUmbralEsCritico() {
        Producto p = TestData.producto(poleras, 1L, 5, 2, 2); // total 4 <= 5
        when(productoRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(p));

        List<ProductoResponse> criticos = service.criticos();

        assertThat(criticos).hasSize(1);
        assertThat(criticos.get(0).stock()).isEqualTo(4);
    }
}
