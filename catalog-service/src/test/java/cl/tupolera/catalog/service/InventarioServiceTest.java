package cl.tupolera.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import cl.tupolera.catalog.dto.ConsultaInventarioRequest;
import cl.tupolera.catalog.dto.ConsultaInventarioResponse;
import cl.tupolera.catalog.dto.DescuentoRequest;
import cl.tupolera.catalog.dto.ItemInventarioRequest;
import cl.tupolera.catalog.dto.ReposicionRequest;
import cl.tupolera.catalog.dto.ResultadoInventarioResponse;
import cl.tupolera.catalog.exception.ApiException;
import cl.tupolera.catalog.exception.InventarioException;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.model.DetalleMovimiento;
import cl.tupolera.catalog.model.MovimientoInventario;
import cl.tupolera.catalog.model.Producto;
import cl.tupolera.catalog.model.TipoMovimiento;
import cl.tupolera.catalog.repository.MovimientoInventarioRepository;
import cl.tupolera.catalog.repository.ProductoRepository;
import cl.tupolera.catalog.repository.ProductoVarianteRepository;

/** Lógica del servicio con repositorios simulados (la concurrencia real se prueba en la prueba de integración). */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final String REF = "3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10";

    @Mock private ProductoRepository productoRepository;
    @Mock private ProductoVarianteRepository varianteRepository;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private InventarioService service;
    private Categoria poleras;

    @BeforeEach
    void preparar() {
        service = new InventarioService(productoRepository, varianteRepository, movimientoRepository,
                transactionManager);
        poleras = TestData.poleras();
    }

    /* ----------------------------- ayudas ----------------------------- */

    /** Producto con tallas S, M y L; los ids de variante son idProducto*10 + posición (S=0, M=1, L=2). */
    private Producto producto(long id, int... stocks) {
        Producto p = TestData.producto(poleras, id, 5, stocks);
        for (int i = 0; i < p.getVariantes().size(); i++) {
            ReflectionTestUtils.setField(p.getVariantes().get(i), "id", id * 10 + i);
        }
        return p;
    }

    private static ItemInventarioRequest item(long productoId, String talla, int cantidad) {
        return new ItemInventarioRequest(productoId, talla, cantidad);
    }

    private static DescuentoRequest descuento(ItemInventarioRequest... items) {
        return new DescuentoRequest(REF, List.of(items));
    }

    private static List<Object[]> filasStock(Object[]... filas) {
        return List.of(filas);
    }

    private void sinMovimientoPrevio() {
        when(movimientoRepository.findByTipoAndClave(any(TipoMovimiento.class), any())).thenReturn(Optional.empty());
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static int status(Throwable t) {
        return ((InventarioException) t).getStatus().value();
    }

    private static List<String> codigos(Throwable t) {
        return ((InventarioException) t).getErrores().stream().map(e -> e.codigo()).toList();
    }

    /* ----------------------------- consulta ----------------------------- */

    @Test
    void consultaVariosProductosConPrecioFinalDelCatalogo() {
        Producto uno = producto(1L, 5, 10, 8);
        uno.setPrecio(12990);
        uno.setDescuento(50);
        Producto dos = producto(2L, 4);
        dos.setActivo(false);
        dos.setPrecio(14990);
        dos.setDescuento(10);

        when(productoRepository.findAllById(List.of(1L, 2L, 99L))).thenReturn(List.of(uno, dos));

        ConsultaInventarioResponse r = service.consultar(new ConsultaInventarioRequest(List.of(1L, 2L, 99L, 1L)));

        assertThat(r.productos()).hasSize(2);
        assertThat(r.noEncontrados()).containsExactly(99L);

        assertThat(r.productos().get(0).precioFinal()).isEqualTo(6495);       // 12990 con 50% de descuento
        assertThat(r.productos().get(0).codigo()).isEqualTo("POL-1");
        assertThat(r.productos().get(0).variantes()).extracting("talla").containsExactly("S", "M", "L");
        assertThat(r.productos().get(0).variantes()).extracting("id").containsExactly(10L, 11L, 12L);
        assertThat(r.productos().get(0).variantes()).extracting("stock").containsExactly(5, 10, 8);

        assertThat(r.productos().get(1).activo()).isFalse();                  // se informa, sales decide
        assertThat(r.productos().get(1).precioFinal()).isEqualTo(13491);     // 14990 con 10% de descuento
    }

    @Test
    void elPrecioFinalUsaLaMismaFormulaDelCatalogo() {
        assertThat(ProductoMapper.precioFinal(12990, 50)).isEqualTo(6495);
        assertThat(ProductoMapper.precioFinal(24990, 25)).isEqualTo(18743);   // 18742.5 se redondea al peso
        assertThat(ProductoMapper.precioFinal(8990, 0)).isEqualTo(8990);
    }

    /* ----------------------------- descuento ----------------------------- */

    @Test
    void descuentaLaTallaSolicitadaYDevuelveElStockActual() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        sinMovimientoPrevio();
        when(varianteRepository.descontar(11L, 2)).thenReturn(1);
        when(varianteRepository.stocksPorIds(anyList())).thenReturn(filasStock(new Object[] { 11L, 8 }));

        ResultadoInventarioResponse r = service.descontar(descuento(item(1, "M", 2)));

        assertThat(r.referencia()).isEqualTo(REF);
        assertThat(r.tipo()).isEqualTo("DESCUENTO");
        assertThat(r.repetida()).isFalse();
        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).varianteId()).isEqualTo(11L);
        assertThat(r.items().get(0).talla()).isEqualTo("M");
        assertThat(r.items().get(0).cantidad()).isEqualTo(2);
        assertThat(r.items().get(0).stockActual()).isEqualTo(8);
        verify(varianteRepository).descontar(11L, 2);
    }

    @Test
    void descuentaVariasVariantesSiempreEnOrdenDeId() {
        Producto p1 = producto(1L, 5, 10, 8);
        Producto p3 = producto(3L, 2, 9, 7);
        when(productoRepository.findAllById(List.of(3L, 1L))).thenReturn(List.of(p3, p1));
        sinMovimientoPrevio();
        when(varianteRepository.descontar(anyLong(), anyInt())).thenReturn(1);
        when(varianteRepository.stocksPorIds(anyList())).thenReturn(filasStock(new Object[] { 30L, 1 },
                new Object[] { 12L, 7 }));

        // Se piden en orden "inverso": 3 antes que 1.
        service.descontar(descuento(item(3, "S", 1), item(1, "L", 1)));

        InOrder orden = Mockito.inOrder(varianteRepository);
        orden.verify(varianteRepository).descontar(12L, 1);   // variante de menor id primero
        orden.verify(varianteRepository).descontar(30L, 1);
    }

    @Test
    void reuneTodosLosErroresYNoModificaNada() {
        Producto activo = producto(1L, 5, 10, 8);
        Producto inactivo = producto(2L, 4);
        inactivo.setActivo(false);
        when(productoRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(activo, inactivo));
        when(movimientoRepository.findByTipoAndClave(any(TipoMovimiento.class), any())).thenReturn(Optional.empty());

        DescuentoRequest solicitud = descuento(
                item(1, "S", 99),    // stock insuficiente (hay 5)
                item(1, "XXL", 1),   // variante inexistente
                item(2, "S", 1),     // producto inactivo
                item(3, "S", 1));    // producto inexistente

        assertThatThrownBy(() -> service.descontar(solicitud)).isInstanceOf(InventarioException.class)
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(404);   // hay productos o tallas que no existen
                    assertThat(codigos(e)).containsExactlyInAnyOrder("STOCK_INSUFICIENTE",
                            "VARIANTE_NO_ENCONTRADA", "PRODUCTO_INACTIVO", "PRODUCTO_NO_ENCONTRADO");
                    assertThat(((InventarioException) e).getErrores().get(0).disponible()).isEqualTo(5);
                });

        verify(varianteRepository, never()).descontar(anyLong(), anyInt());
        verify(movimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void stockInsuficienteOProductoInactivoResponde409() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(movimientoRepository.findByTipoAndClave(any(TipoMovimiento.class), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descontar(descuento(item(1, "S", 6))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("STOCK_INSUFICIENTE");
                });

        p.setActivo(false);
        assertThatThrownBy(() -> service.descontar(descuento(item(1, "S", 1))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("PRODUCTO_INACTIVO");
                });
    }

    @Test
    void siElUpdateNoCumpleLaCondicionSeRevierteTodaLaTransaccion() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        sinMovimientoPrevio();
        // Entre la validación y el UPDATE otra compra se llevó el stock de la talla M (id 11).
        when(varianteRepository.descontar(10L, 1)).thenReturn(1);
        when(varianteRepository.descontar(11L, 2)).thenReturn(0);
        when(varianteRepository.stocksPorIds(anyList())).thenReturn(filasStock(new Object[] { 11L, 1 }));

        assertThatThrownBy(() -> service.descontar(descuento(item(1, "S", 1), item(1, "M", 2))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("STOCK_INSUFICIENTE");
                });

        // La excepción hace que TransactionTemplate pida el rollback (en la base real deshace el UPDATE de S).
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void rechazaItemsRepetidosYReferenciasInvalidas() {
        assertThatThrownBy(() -> service.descontar(descuento(item(1, "M", 1), item(1, " M ", 2))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(400));

        assertThatThrownBy(() -> service.descontar(new DescuentoRequest("corta", List.of(item(1, "M", 1)))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(400));

        assertThatThrownBy(() -> service.revertirDescuento("con espacios y simbolos!"))
                .isInstanceOf(ApiException.class);
    }

    /* ----------------------------- idempotencia ----------------------------- */

    private MovimientoInventario descuentoPrevio(List<ItemInventarioRequest> items) {
        MovimientoInventario previo = new MovimientoInventario(TipoMovimiento.DESCUENTO, REF,
                InventarioService.calcularHuella(items));
        previo.getDetalles().add(new DetalleMovimiento(1L, 11L, "M", 2, 8));
        return previo;
    }

    @Test
    void repetirLaMismaReferenciaDevuelveElResultadoSinVolverADescontar() {
        List<ItemInventarioRequest> items = List.of(item(1, "M", 2));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoPrevio(items)));
        when(movimientoRepository.existsByTipoAndClave(TipoMovimiento.REVERSION, REF)).thenReturn(false);

        ResultadoInventarioResponse r = service.descontar(descuento(item(1, "M", 2)));

        assertThat(r.repetida()).isTrue();
        assertThat(r.items().get(0).stockActual()).isEqualTo(8);
        verify(varianteRepository, never()).descontar(anyLong(), anyInt());
        verify(movimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void reutilizarLaReferenciaConOtroContenidoEsUnConflicto() {
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoPrevio(List.of(item(1, "M", 2)))));

        assertThatThrownBy(() -> service.descontar(descuento(item(1, "M", 5))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("CONFLICTO_IDEMPOTENCIA");
                });
        verify(varianteRepository, never()).descontar(anyLong(), anyInt());
    }

    @Test
    void unDescuentoYaRevertidoNoSePuedeReproducir() {
        List<ItemInventarioRequest> items = List.of(item(1, "M", 2));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoPrevio(items)));
        when(movimientoRepository.existsByTipoAndClave(TipoMovimiento.REVERSION, REF)).thenReturn(true);

        assertThatThrownBy(() -> service.descontar(descuento(item(1, "M", 2))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("REFERENCIA_REVERTIDA");
                });
    }

    @Test
    void siOtraSolicitudIgualGanaLaClaveSeDevuelveSuResultado() {
        List<ItemInventarioRequest> items = List.of(item(1, "M", 2));
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        // 1ª lectura: no existe; al intentar registrar choca con la restricción única; 2ª lectura: ya existe.
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.empty(), Optional.of(descuentoPrevio(items)));
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));
        when(movimientoRepository.existsByTipoAndClave(TipoMovimiento.REVERSION, REF)).thenReturn(false);

        ResultadoInventarioResponse r = service.descontar(descuento(item(1, "M", 2)));

        assertThat(r.repetida()).isTrue();
        verify(varianteRepository, never()).descontar(anyLong(), anyInt());
    }

    @Test
    void unErrorDeIntegridadQueNoEsUnDuplicadoSePropaga() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF)).thenReturn(Optional.empty());
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class)))
                .thenThrow(new DataIntegrityViolationException("otro error"));

        assertThatThrownBy(() -> service.descontar(descuento(item(1, "M", 2))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reintentaSiMySqlAbortaLaTransaccionPorUnDeadlock() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenThrow(new CannotAcquireLockException("Deadlock found"))
                .thenReturn(Optional.empty());
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(varianteRepository.descontar(11L, 2)).thenReturn(1);
        when(varianteRepository.stocksPorIds(anyList())).thenReturn(filasStock(new Object[] { 11L, 8 }));

        ResultadoInventarioResponse r = service.descontar(descuento(item(1, "M", 2)));

        assertThat(r.repetida()).isFalse();
        verify(varianteRepository).descontar(11L, 2);
    }

    /* ----------------------------- reposición ----------------------------- */

    private static ReposicionRequest reposicion(String clave, ItemInventarioRequest... items) {
        return new ReposicionRequest(clave, List.of(items));
    }

    @Test
    void reponeUnidadesDeUnaVariante() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        sinMovimientoPrevio();
        when(varianteRepository.reponer(11L, 4, InventarioService.STOCK_MAXIMO - 4)).thenReturn(1);
        when(varianteRepository.stocksPorIds(anyList())).thenReturn(filasStock(new Object[] { 11L, 14 }));

        ResultadoInventarioResponse r = service.reponer(reposicion(REF, item(1, "M", 4)));

        assertThat(r.tipo()).isEqualTo("REPOSICION");
        assertThat(r.repetida()).isFalse();
        assertThat(r.items().get(0).stockActual()).isEqualTo(14);
    }

    @Test
    void reponerPermiteProductosInactivosPeroNoVariantesInexistentes() {
        Producto inactivo = producto(1L, 5, 10, 8);
        inactivo.setActivo(false);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(inactivo));
        sinMovimientoPrevio();
        when(varianteRepository.reponer(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(varianteRepository.stocksPorIds(anyList())).thenReturn(filasStock(new Object[] { 10L, 6 }));

        assertThat(service.reponer(reposicion(REF, item(1, "S", 1))).items()).hasSize(1);

        assertThatThrownBy(() -> service.reponer(reposicion(REF, item(1, "XXL", 1))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(404);
                    assertThat(codigos(e)).containsExactly("VARIANTE_NO_ENCONTRADA");
                });
    }

    @Test
    void reponerRechazaSuperarElStockMaximo() {
        Producto p = producto(1L, 5, 10, 8);
        when(productoRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        sinMovimientoPrevio();
        when(varianteRepository.reponer(anyLong(), anyInt(), anyInt())).thenReturn(0);

        assertThatThrownBy(() -> service.reponer(reposicion(REF, item(1, "M", 10000))))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("LIMITE_STOCK");
                });
    }

    @Test
    void repetirUnaReposicionNoVuelveASumar() {
        List<ItemInventarioRequest> items = List.of(item(1, "M", 4));
        MovimientoInventario previa = new MovimientoInventario(TipoMovimiento.REPOSICION, REF,
                InventarioService.calcularHuella(items));
        previa.getDetalles().add(new DetalleMovimiento(1L, 11L, "M", 4, 14));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.REPOSICION, REF)).thenReturn(Optional.of(previa));

        ResultadoInventarioResponse r = service.reponer(reposicion(REF, item(1, "M", 4)));

        assertThat(r.repetida()).isTrue();
        verify(varianteRepository, never()).reponer(anyLong(), anyInt(), anyInt());

        // La misma clave con otra cantidad es un conflicto.
        assertThatThrownBy(() -> service.reponer(reposicion(REF, item(1, "M", 9))))
                .satisfies(e -> assertThat(codigos(e)).containsExactly("CONFLICTO_IDEMPOTENCIA"));
    }

    /* ----------------------------- reversión ----------------------------- */

    private MovimientoInventario descuentoConDosVariantes() {
        MovimientoInventario d = new MovimientoInventario(TipoMovimiento.DESCUENTO, REF, "x".repeat(64));
        d.getDetalles().add(new DetalleMovimiento(3L, 30L, "S", 1, 1));
        d.getDetalles().add(new DetalleMovimiento(1L, 11L, "M", 2, 8));
        return d;
    }

    @Test
    void revierteExactamenteLasCantidadesDelDescuentoOriginal() {
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoConDosVariantes()));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.REVERSION, REF)).thenReturn(Optional.empty());
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(varianteRepository.reponer(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(varianteRepository.stocksPorIds(anyList()))
                .thenReturn(filasStock(new Object[] { 11L, 10 }, new Object[] { 30L, 2 }));

        ResultadoInventarioResponse r = service.revertirDescuento(REF);

        assertThat(r.tipo()).isEqualTo("REVERSION");
        assertThat(r.repetida()).isFalse();
        assertThat(r.items()).extracting("cantidad").containsExactly(2, 1);   // en orden de id: 11 y 30

        InOrder orden = Mockito.inOrder(varianteRepository);
        orden.verify(varianteRepository).reponer(eq(11L), eq(2), anyInt());
        orden.verify(varianteRepository).reponer(eq(30L), eq(1), anyInt());
    }

    @Test
    void repetirLaReversionNoVuelveASumar() {
        MovimientoInventario previa = new MovimientoInventario(TipoMovimiento.REVERSION, REF,
                InventarioServiceTestAccesos.huellaReversion());
        previa.getDetalles().add(new DetalleMovimiento(1L, 11L, "M", 2, 10));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoConDosVariantes()));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.REVERSION, REF)).thenReturn(Optional.of(previa));

        ResultadoInventarioResponse r = service.revertirDescuento(REF);

        assertThat(r.repetida()).isTrue();
        verify(varianteRepository, never()).reponer(anyLong(), anyInt(), anyInt());
        verify(movimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void revertirUnaReferenciaSinDescuentoResponde404() {
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revertirDescuento(REF))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(404);
                    assertThat(codigos(e)).containsExactly("DESCUENTO_NO_ENCONTRADO");
                });
        verify(varianteRepository, never()).reponer(anyLong(), anyInt(), anyInt());
    }

    @Test
    void siUnaVarianteYaNoExisteLaReversionFallaCompleta() {
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoConDosVariantes()));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.REVERSION, REF)).thenReturn(Optional.empty());
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(varianteRepository.reponer(eq(11L), anyInt(), anyInt())).thenReturn(1);
        when(varianteRepository.reponer(eq(30L), anyInt(), anyInt())).thenReturn(0);

        assertThatThrownBy(() -> service.revertirDescuento(REF))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(codigos(e)).containsExactly("VARIANTE_NO_ENCONTRADA");
                });
        verify(transactionManager).rollback(any());
    }

    @Test
    void siOtraReversionGanaLaClaveSeDevuelveSuResultado() {
        MovimientoInventario previa = new MovimientoInventario(TipoMovimiento.REVERSION, REF,
                InventarioServiceTestAccesos.huellaReversion());
        previa.getDetalles().add(new DetalleMovimiento(1L, 11L, "M", 2, 10));

        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.DESCUENTO, REF))
                .thenReturn(Optional.of(descuentoConDosVariantes()));
        when(movimientoRepository.findByTipoAndClave(TipoMovimiento.REVERSION, REF))
                .thenReturn(Optional.empty(), Optional.of(previa));
        when(movimientoRepository.saveAndFlush(any(MovimientoInventario.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        ResultadoInventarioResponse r = service.revertirDescuento(REF);

        assertThat(r.repetida()).isTrue();
        verify(varianteRepository, never()).reponer(anyLong(), anyInt(), anyInt());
    }

    /** Acceso a la huella constante de las reversiones (se calcula igual que en el servicio). */
    private static final class InventarioServiceTestAccesos {
        static String huellaReversion() {
            return (String) ReflectionTestUtils.getField(InventarioService.class, "HUELLA_REVERSION");
        }
    }
}
