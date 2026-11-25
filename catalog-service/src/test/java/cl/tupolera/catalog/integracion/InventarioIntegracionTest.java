package cl.tupolera.catalog.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import cl.tupolera.catalog.dto.ConsultaInventarioRequest;
import cl.tupolera.catalog.dto.ConsultaInventarioResponse;
import cl.tupolera.catalog.dto.DescuentoRequest;
import cl.tupolera.catalog.dto.ItemInventarioRequest;
import cl.tupolera.catalog.dto.ReposicionRequest;
import cl.tupolera.catalog.dto.ResultadoInventarioResponse;
import cl.tupolera.catalog.exception.InventarioException;
import cl.tupolera.catalog.repository.MovimientoInventarioRepository;
import cl.tupolera.catalog.repository.ProductoRepository;
import cl.tupolera.catalog.repository.ProductoVarianteRepository;
import cl.tupolera.catalog.service.InventarioService;

/**
 * Pruebas contra un MySQL 8 REAL y desechable (Testcontainers). No tocan tus datos de desarrollo: el contenedor
 * se crea y se destruye en cada ejecución. Son las únicas que demuestran de verdad la atomicidad y la
 * concurrencia. Requieren Docker en ejecución; si Docker no está disponible se OMITEN y el resumen final lo
 * informa (ver ResumenIntegracionDockerListener). Un BUILD SUCCESS con estas pruebas omitidas NO valida la concurrencia.
 *
 * Usan los productos de ejemplo del perfil dev (1: Urban Red S/M/L/XL, 3: Black Hoodie S/M/L/XL).
 */
@Tag("integracion-docker")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=secreto-de-pruebas-de-integracion-con-mas-de-32-bytes",
        "app.jwt.issuer=tupolera-test",
        "app.internal.api-key=" + InventarioIntegracionTest.CLAVE
})
@AutoConfigureMockMvc
class InventarioIntegracionTest {

    static final String CLAVE = "clave-interna-de-integracion-32-caracteres-min";
    private static final long UNO = 1L;   // Urban Red
    private static final long TRES = 3L;  // Black Hoodie

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired private InventarioService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoVarianteRepository varianteRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;

    /* ----------------------------- ayudas ----------------------------- */

    @BeforeEach
    void restaurarDatosDeLaPrueba() {
        jdbc.update("DELETE FROM movimiento_inventario");
        jdbc.update("UPDATE producto SET activo = 1 WHERE id IN (1, 2, 3)");
        fijarStock(UNO, "S", 5);
        fijarStock(UNO, "M", 10);
        fijarStock(UNO, "L", 8);
        fijarStock(UNO, "XL", 3);
        fijarStock(TRES, "S", 2);
        fijarStock(TRES, "M", 9);
    }

    private void fijarStock(long productoId, String talla, int stock) {
        jdbc.update("UPDATE producto_variante SET stock = ? WHERE producto_id = ? AND talla = ?",
                stock, productoId, talla);
    }

    private int stock(long productoId, String talla) {
        return jdbc.queryForObject("SELECT stock FROM producto_variante WHERE producto_id = ? AND talla = ?",
                Integer.class, productoId, talla);
    }

    private long contarMovimientos(String tipo) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM movimiento_inventario WHERE tipo = ?", Long.class, tipo);
    }

    private static ItemInventarioRequest item(long productoId, String talla, int cantidad) {
        return new ItemInventarioRequest(productoId, talla, cantidad);
    }

    private static String nuevaReferencia() {
        return UUID.randomUUID().toString();
    }

    private static DescuentoRequest descuento(String referencia, ItemInventarioRequest... items) {
        return new DescuentoRequest(referencia, List.of(items));
    }

    /** Ejecuta n tareas a la vez (todas esperan una señal de salida común) y devuelve su resultado o su excepción. */
    private List<Object> enParalelo(int hilos, IntFunction<Callable<ResultadoInventarioResponse>> fabrica)
            throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            Callable<ResultadoInventarioResponse> tarea = fabrica.apply(i);
            futuros.add(pool.submit(() -> {
                listos.countDown();
                salida.await();
                try {
                    return tarea.call();
                } catch (InventarioException e) {
                    return e;
                }
            }));
        }

        listos.await();
        salida.countDown();

        List<Object> resultados = new ArrayList<>();
        for (Future<Object> futuro : futuros) {
            resultados.add(futuro.get(90, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return resultados;
    }

    private static long exitosos(List<Object> resultados) {
        return resultados.stream().filter(r -> r instanceof ResultadoInventarioResponse).count();
    }

    private static long rechazadosPor409(List<Object> resultados) {
        return resultados.stream()
                .filter(r -> r instanceof InventarioException e && e.getStatus().value() == 409).count();
    }

    /* ----------------------------- consulta y descuento ----------------------------- */

    @Test
    void consultaVariosProductosConPrecioFinalDelCatalogo() {
        ConsultaInventarioResponse r = service.consultar(new ConsultaInventarioRequest(List.of(UNO, TRES, 999L)));

        assertThat(r.productos()).hasSize(2);
        assertThat(r.noEncontrados()).containsExactly(999L);
        assertThat(r.productos().get(0).precioFinal()).isEqualTo(6495);     // 12990 con 50% de descuento
        assertThat(r.productos().get(0).variantes()).extracting("talla").containsExactly("S", "M", "L", "XL");
        assertThat(r.productos().get(0).variantes()).extracting("stock").containsExactly(5, 10, 8, 3);
    }

    @Test
    void descuentaLaTallaCorrectaYRegistraElMovimiento() {
        ResultadoInventarioResponse r = service.descontar(descuento(nuevaReferencia(), item(UNO, "M", 4)));

        assertThat(r.repetida()).isFalse();
        assertThat(r.items().get(0).stockActual()).isEqualTo(6);
        assertThat(stock(UNO, "M")).isEqualTo(6);
        assertThat(stock(UNO, "S")).isEqualTo(5);   // las demás tallas no cambian
        assertThat(contarMovimientos("DESCUENTO")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM producto_variante WHERE producto_id = 1 AND talla = 'M'",
                Long.class)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void sinStockSuficienteNoDescuentaNingunaVarianteYNoDejaRegistro() {
        // El segundo ítem no alcanza: el primero NO debe quedar descontado.
        assertThatThrownBy(() -> service.descontar(descuento(nuevaReferencia(),
                item(UNO, "M", 2), item(UNO, "S", 99))))
                .isInstanceOfSatisfying(InventarioException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(409);
                    assertThat(e.getErrores()).extracting("codigo").containsExactly("STOCK_INSUFICIENTE");
                });

        assertThat(stock(UNO, "M")).isEqualTo(10);
        assertThat(stock(UNO, "S")).isEqualTo(5);
        assertThat(contarMovimientos("DESCUENTO")).isZero();
    }

    @Test
    void siFallaElUpdateDeUnaVarianteSeRevierteElDeLasAnteriores() {
        // Se simula la carrera: la validación pasó, pero el UPDATE de la 2ª variante (S de Black Hoodie) no
        // encuentra stock. El 1º (S de Urban Red) ya se ejecutó de verdad en la base y debe deshacerse.
        long idUrbanS = jdbc.queryForObject("SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'S'", Long.class);
        long idHoodieS = jdbc.queryForObject("SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'S'", Long.class);
        assertThat(idUrbanS).isLessThan(idHoodieS);   // el servicio actualiza en orden de id

        ProductoVarianteRepository envoltorio = Mockito.mock(ProductoVarianteRepository.class,
                AdditionalAnswers.delegatesTo(varianteRepository));
        Mockito.doReturn(0).when(envoltorio).descontar(eq(idHoodieS), anyInt());

        InventarioService conFalla = new InventarioService(productoRepository, envoltorio, movimientoRepository,
                transactionManager);

        assertThatThrownBy(() -> conFalla.descontar(descuento(nuevaReferencia(),
                item(UNO, "S", 1), item(TRES, "S", 1))))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(409));

        assertThat(stock(UNO, "S")).isEqualTo(5);     // el UPDATE ya ejecutado fue deshecho
        assertThat(stock(TRES, "S")).isEqualTo(2);
        assertThat(contarMovimientos("DESCUENTO")).isZero();
    }

    @Test
    void rechazaProductosInactivosSinTocarElStock() {
        jdbc.update("UPDATE producto SET activo = 0 WHERE id = 3");

        assertThatThrownBy(() -> service.descontar(descuento(nuevaReferencia(),
                item(UNO, "M", 1), item(TRES, "M", 1))))
                .isInstanceOfSatisfying(InventarioException.class, e ->
                        assertThat(e.getErrores()).extracting("codigo").containsExactly("PRODUCTO_INACTIVO"));

        assertThat(stock(UNO, "M")).isEqualTo(10);
        assertThat(stock(TRES, "M")).isEqualTo(9);
    }

    /* ----------------------------- concurrencia ----------------------------- */

    @Test
    void veinteComprasSimultaneasNuncaVendenMasUnidadesDeLasQueHay() throws Exception {
        fijarStock(UNO, "M", 10);

        // 20 compras distintas (referencias distintas) de 1 unidad cada una, todas a la vez, con stock 10.
        List<Object> resultados = enParalelo(20, i -> () ->
                service.descontar(descuento(nuevaReferencia(), item(UNO, "M", 1))));

        assertThat(exitosos(resultados)).isEqualTo(10);
        assertThat(rechazadosPor409(resultados)).isEqualTo(10);
        assertThat(stock(UNO, "M")).isZero();              // jamás negativo
        assertThat(contarMovimientos("DESCUENTO")).isEqualTo(10);
    }

    @Test
    void laMismaReferenciaEnParaleloSeDescuentaUnaSolaVez() throws Exception {
        String referencia = nuevaReferencia();

        List<Object> resultados = enParalelo(10, i -> () ->
                service.descontar(descuento(referencia, item(UNO, "M", 3))));

        // Todas responden bien (una aplica, las demás reciben el resultado original) y se descuenta una vez.
        assertThat(exitosos(resultados)).isEqualTo(10);
        long aplicadas = resultados.stream()
                .map(r -> (ResultadoInventarioResponse) r).filter(r -> !r.repetida()).count();
        assertThat(aplicadas).isEqualTo(1);
        assertThat(stock(UNO, "M")).isEqualTo(7);
        assertThat(contarMovimientos("DESCUENTO")).isEqualTo(1);
    }

    @Test
    void solicitudesConLosItemsEnOrdenOpuestoNoCausanDeadlock() throws Exception {
        fijarStock(UNO, "M", 100);
        fijarStock(TRES, "M", 100);

        List<Object> resultados = enParalelo(20, i -> () -> i % 2 == 0
                ? service.descontar(descuento(nuevaReferencia(), item(UNO, "M", 1), item(TRES, "M", 1)))
                : service.descontar(descuento(nuevaReferencia(), item(TRES, "M", 1), item(UNO, "M", 1))));

        assertThat(exitosos(resultados)).isEqualTo(20);
        assertThat(stock(UNO, "M")).isEqualTo(80);
        assertThat(stock(TRES, "M")).isEqualTo(80);
    }

    @Test
    void descuentosMultiplesConcurrentesSonAtomicosPorSolicitud() throws Exception {
        // Urban Red L tiene 8 y XL tiene 3. Cada solicitud pide 1 de L y 1 de XL: solo 3 pueden completarse
        // (XL se agota). Las demás deben fallar COMPLETAS: no puede quedar L descontado sin XL.
        List<Object> resultados = enParalelo(12, i -> () ->
                service.descontar(descuento(nuevaReferencia(), item(UNO, "L", 1), item(UNO, "XL", 1))));

        assertThat(exitosos(resultados)).isEqualTo(3);
        assertThat(stock(UNO, "XL")).isZero();
        assertThat(stock(UNO, "L")).isEqualTo(5);          // 8 - 3: ni una unidad de L "perdida"
    }

    /* ----------------------------- reposición ----------------------------- */

    @Test
    void reponeYNoAplicaDosVecesLaMismaClave() {
        String clave = nuevaReferencia();
        ReposicionRequest solicitud = new ReposicionRequest(clave, List.of(item(UNO, "S", 7)));

        ResultadoInventarioResponse primera = service.reponer(solicitud);
        ResultadoInventarioResponse segunda = service.reponer(solicitud);

        assertThat(primera.repetida()).isFalse();
        assertThat(primera.items().get(0).stockActual()).isEqualTo(12);
        assertThat(segunda.repetida()).isTrue();
        assertThat(stock(UNO, "S")).isEqualTo(12);          // sumó una sola vez
        assertThat(contarMovimientos("REPOSICION")).isEqualTo(1);

        // La misma clave con otra cantidad es un conflicto y no cambia el stock.
        assertThatThrownBy(() -> service.reponer(new ReposicionRequest(clave, List.of(item(UNO, "S", 50)))))
                .isInstanceOfSatisfying(InventarioException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(409);
                    assertThat(e.getErrores()).extracting("codigo").containsExactly("CONFLICTO_IDEMPOTENCIA");
                });
        assertThat(stock(UNO, "S")).isEqualTo(12);
    }

    @Test
    void reposicionesDuplicadasEnParaleloSeAplicanUnaSolaVez() throws Exception {
        String clave = nuevaReferencia();

        List<Object> resultados = enParalelo(10, i -> () ->
                service.reponer(new ReposicionRequest(clave, List.of(item(UNO, "S", 5)))));

        assertThat(exitosos(resultados)).isEqualTo(10);
        assertThat(stock(UNO, "S")).isEqualTo(10);          // 5 + 5, una sola reposición
        assertThat(contarMovimientos("REPOSICION")).isEqualTo(1);
    }

    @Test
    void reponerUnaTallaInexistenteResponde404() {
        assertThatThrownBy(() -> service.reponer(new ReposicionRequest(nuevaReferencia(),
                List.of(item(UNO, "XXL", 1)))))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(404));
    }

    /* ----------------------------- reversión ----------------------------- */

    @Test
    void laReversionDevuelveExactamenteLoDescontadoYSoloUnaVez() {
        String referencia = nuevaReferencia();
        service.descontar(descuento(referencia, item(UNO, "M", 4), item(TRES, "S", 1)));
        assertThat(stock(UNO, "M")).isEqualTo(6);
        assertThat(stock(TRES, "S")).isEqualTo(1);

        // Mientras tanto otra operación cambia el stock: la reversión suma solo lo descontado, no "restaura" un total.
        service.reponer(new ReposicionRequest(nuevaReferencia(), List.of(item(UNO, "M", 10))));
        assertThat(stock(UNO, "M")).isEqualTo(16);

        ResultadoInventarioResponse primera = service.revertirDescuento(referencia);
        ResultadoInventarioResponse segunda = service.revertirDescuento(referencia);

        assertThat(primera.repetida()).isFalse();
        assertThat(primera.items()).extracting("cantidad").containsExactlyInAnyOrder(4, 1);
        assertThat(segunda.repetida()).isTrue();
        assertThat(stock(UNO, "M")).isEqualTo(20);         // 16 + 4, una sola vez
        assertThat(stock(TRES, "S")).isEqualTo(2);         // 1 + 1
        assertThat(contarMovimientos("REVERSION")).isEqualTo(1);
    }

    @Test
    void reversionesSimultaneasDelMismoDescuentoSumanUnaSolaVez() throws Exception {
        String referencia = nuevaReferencia();
        service.descontar(descuento(referencia, item(UNO, "M", 4)));
        assertThat(stock(UNO, "M")).isEqualTo(6);

        List<Object> resultados = enParalelo(10, i -> () -> service.revertirDescuento(referencia));

        assertThat(exitosos(resultados)).isEqualTo(10);
        long aplicadas = resultados.stream()
                .map(r -> (ResultadoInventarioResponse) r).filter(r -> !r.repetida()).count();
        assertThat(aplicadas).isEqualTo(1);
        assertThat(stock(UNO, "M")).isEqualTo(10);          // volvió al valor original, no 14
        assertThat(contarMovimientos("REVERSION")).isEqualTo(1);
    }

    @Test
    void revertirUnaReferenciaDesconocidaResponde404YUnDescuentoRevertidoNoSeReproduce() {
        assertThatThrownBy(() -> service.revertirDescuento(nuevaReferencia()))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(404));

        String referencia = nuevaReferencia();
        DescuentoRequest solicitud = descuento(referencia, item(UNO, "S", 1));
        service.descontar(solicitud);
        service.revertirDescuento(referencia);

        assertThatThrownBy(() -> service.descontar(solicitud))
                .isInstanceOfSatisfying(InventarioException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(409);
                    assertThat(e.getErrores()).extracting("codigo").containsExactly("REFERENCIA_REVERTIDA");
                });
        assertThat(stock(UNO, "S")).isEqualTo(5);
    }

    /* ----------------------------- HTTP completo ----------------------------- */

    @Test
    void elEndpointHttpExigeLaClaveYDescuentaDeVerdad() throws Exception {
        String cuerpo = "{\"referencia\":\"" + nuevaReferencia()
                + "\",\"items\":[{\"productoId\":1,\"talla\":\"L\",\"cantidad\":2}]}";

        mockMvc.perform(post("/internal/inventario/descuento").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        assertThat(stock(UNO, "L")).isEqualTo(8);

        // Un JWT de administrador tampoco da acceso a /internal.
        mockMvc.perform(post("/internal/inventario/descuento")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        assertThat(stock(UNO, "L")).isEqualTo(8);

        mockMvc.perform(post("/internal/inventario/descuento").header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DESCUENTO"))
                .andExpect(jsonPath("$.items[0].stockActual").value(6));
        assertThat(stock(UNO, "L")).isEqualTo(6);
    }

    @Test
    void elEndpointResponde409ConElDetalleDeLosErrores() throws Exception {
        String cuerpo = "{\"referencia\":\"" + nuevaReferencia()
                + "\",\"items\":[{\"productoId\":1,\"talla\":\"S\",\"cantidad\":99}]}";

        mockMvc.perform(post("/internal/inventario/descuento").header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores[0].codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.errores[0].disponible").value(5));
    }
}
