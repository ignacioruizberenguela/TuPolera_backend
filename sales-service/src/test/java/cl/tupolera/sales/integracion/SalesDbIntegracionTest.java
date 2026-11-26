package cl.tupolera.sales.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Pruebas contra un MySQL 8 REAL y desechable (Testcontainers): comprueban que el esquema sales_db se crea bien,
 * que la configuración de despacho se guarda y se usa de verdad, y que las restricciones de las tablas funcionan
 * (claves únicas, idempotencia, CHECK de montos y estados). No tocan tus datos de desarrollo.
 *
 * Nota: MySQL informa las violaciones de CHECK con SQLSTATE HY000, que Spring no clasifica como
 * DataIntegrityViolationException; por eso se comprueba DataAccessException y el nombre de la restricción.
 *
 * Requieren Docker en ejecución. Si no hay Docker se OMITEN y el resumen final de la ejecución lo informa
 * (ver ResumenIntegracionDockerListener): un BUILD SUCCESS con estas pruebas omitidas NO las valida.
 */
@Tag("integracion-docker")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=secreto-de-pruebas-de-integracion-con-mas-de-32-bytes",
        "app.jwt.issuer=tupolera-test"
})
@AutoConfigureMockMvc
class SalesDbIntegracionTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private static final String CONFIGURACION_NUEVA = """
            {"nombreBodega":"Bodega Central","direccionBodega":"Av. Siempre Viva 742, Santiago",
             "latitudBodega":-33.45,"longitudBodega":-70.66,"precioBase":1000,"costoPorKm":0,
             "esUbicacionPrueba":false}
            """;

    private static RequestPostProcessor como(String rol, String idUsuario) {
        return jwt().jwt(token -> token.subject(idUsuario)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @BeforeEach
    void restaurar() {
        jdbc.update("DELETE FROM boleta");   // las filas de boleta_detalle y boleta_evento se borran en cascada
        jdbc.update("UPDATE configuracion_despacho SET nombre_bodega = ?, direccion_bodega = ?, "
                        + "latitud_bodega = ?, longitud_bodega = ?, precio_base = 2990, costo_por_km = 150, "
                        + "tarifa_minima = 2990, tarifa_maxima = 15000, es_ubicacion_prueba = 1, "
                        + "actualizado_por = NULL WHERE id = 1",
                "Bodega de prueba Tu Polera", "Calle Ficticia 1234, Santiago, Región Metropolitana",
                -33.4372, -70.6506);
    }

    /* ----------------------------- configuración y cotización ----------------------------- */

    @Test
    void laConfiguracionInicialSeCargaDesdeElEsquema() throws Exception {
        mockMvc.perform(get("/api/v1/despacho/configuracion").with(como("CLIENTE", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreBodega").value("Bodega de prueba Tu Polera"))
                .andExpect(jsonPath("$.latitudBodega").value(-33.4372))
                .andExpect(jsonPath("$.longitudBodega").value(-70.6506))
                .andExpect(jsonPath("$.precioBase").value(2990))
                .andExpect(jsonPath("$.costoPorKm").value(150))
                .andExpect(jsonPath("$.tarifaMinima").value(2990))
                .andExpect(jsonPath("$.tarifaMaxima").value(15000))
                .andExpect(jsonPath("$.esUbicacionPrueba").value(true));
    }

    @Test
    void cotizaConLaConfiguracionGuardadaEnLaBaseDeDatos() throws Exception {
        mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como("CLIENTE", "1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direccion\":\"Av. Providencia 1234\",\"latitud\":-33.4251,\"longitud\":-70.6167}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanciaKm").value(3.421))
                .andExpect(jsonPath("$.precio").value(3500))
                .andExpect(jsonPath("$.tipoCalculo").value("HAVERSINE_LINEA_RECTA"))
                .andExpect(jsonPath("$.origen.esUbicacionPrueba").value(true));

        mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como("CLIENTE", "1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direccion\":\"Valparaíso\",\"latitud\":-33.0472,\"longitud\":-71.6127}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(15000));    // tarifa máxima
    }

    @Test
    void unDestinoFueraDeChileResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como("CLIENTE", "1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direccion\":\"Buenos Aires\",\"latitud\":-34.6037,\"longitud\":-58.3816}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elAdministradorModificaLaConfiguracionYQuedaGuardadaYEnUso() throws Exception {
        mockMvc.perform(put("/api/v1/despacho/configuracion").with(como("ADMINISTRADOR", "7"))
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION_NUEVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreBodega").value("Bodega Central"))
                .andExpect(jsonPath("$.esUbicacionPrueba").value(false));

        // Persistió en MySQL y registró al administrador que la cambió.
        assertThat(jdbc.queryForObject("SELECT nombre_bodega FROM configuracion_despacho WHERE id = 1", String.class))
                .isEqualTo("Bodega Central");
        assertThat(jdbc.queryForObject("SELECT precio_base FROM configuracion_despacho WHERE id = 1", Long.class))
                .isEqualTo(1000L);
        assertThat(jdbc.queryForObject("SELECT tarifa_minima FROM configuracion_despacho WHERE id = 1", Long.class))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT actualizado_por FROM configuracion_despacho WHERE id = 1", Long.class))
                .isEqualTo(7L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM configuracion_despacho", Long.class)).isEqualTo(1L);

        // Las cotizaciones siguientes usan la nueva configuración (base 1000, sin costo por km, sin límites).
        mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como("CLIENTE", "1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direccion\":\"Providencia\",\"latitud\":-33.4251,\"longitud\":-70.6167}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(1000))
                .andExpect(jsonPath("$.origen.nombre").value("Bodega Central"));
    }

    @Test
    void unClienteNoPuedeModificarLaConfiguracion() throws Exception {
        mockMvc.perform(put("/api/v1/despacho/configuracion").with(como("CLIENTE", "1"))
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION_NUEVA))
                .andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT precio_base FROM configuracion_despacho WHERE id = 1", Long.class))
                .isEqualTo(2990L);
    }

    @Test
    void sinTokenNoSePuedeConsultar() throws Exception {
        mockMvc.perform(get("/api/v1/despacho/configuracion")).andExpect(status().isUnauthorized());
    }

    /* ----------------------------- restricciones del esquema ----------------------------- */

    @Test
    void laBaseDeDatosRechazaConfiguracionesIncoherentes() {
        // Tarifa máxima menor que la mínima.
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE configuracion_despacho SET tarifa_minima = 5000, tarifa_maxima = 4000 WHERE id = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_config_tarifas");

        // Montos negativos.
        assertThatThrownBy(() -> jdbc.update("UPDATE configuracion_despacho SET precio_base = -1 WHERE id = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_config_montos");

        // Solo puede existir la fila id = 1.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO configuracion_despacho (id, nombre_bodega, direccion_bodega, latitud_bodega, "
                        + "longitud_bodega, precio_base, costo_por_km, es_ubicacion_prueba, fecha_actualizacion) "
                        + "VALUES (2, 'Otra', 'Otra', -33, -70, 1, 1, 1, UTC_TIMESTAMP())"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_config_fila_unica");
    }

    private long insertarBoleta(String operacion, long usuarioId, String clave, String estado, String inventario,
            String referencia, long subtotal, long despacho, long total, long neto, long iva) {

        jdbc.update("INSERT INTO boleta (numero_operacion, usuario_id, comprador_nombre, comprador_correo, "
                + "comprador_run, fecha, fecha_actualizacion, estado, estado_inventario, resultado_pago, "
                + "referencia_descuento, clave_idempotencia, huella_solicitud, subtotal, precio_despacho, total, "
                + "neto, iva, despacho_direccion, despacho_latitud, despacho_longitud, despacho_distancia_km, "
                + "despacho_tipo_calculo, origen_nombre, origen_direccion, origen_latitud, origen_longitud) "
                + "VALUES (?, ?, 'Juan Pérez', 'juan@gmail.com', '363229777', UTC_TIMESTAMP(), UTC_TIMESTAMP(), "
                + "?, ?, 'APROBADO', ?, ?, ?, ?, ?, ?, ?, ?, 'Av. Providencia 1234', -33.4251, -70.6167, 3.421, "
                + "'HAVERSINE_LINEA_RECTA', 'Bodega', 'Calle 1', -33.4372, -70.6506)",
                operacion, usuarioId, estado, inventario, referencia, clave, "a".repeat(64),
                subtotal, despacho, total, neto, iva);

        return jdbc.queryForObject("SELECT id FROM boleta WHERE numero_operacion = ?", Long.class, operacion);
    }

    private long boletaAprobada(String operacion, long usuarioId, String clave) {
        // productos 6495 (neto 5458 + IVA 1037) + despacho 3500 = 9995
        return insertarBoleta(operacion, usuarioId, clave, "APROBADA", "DESCONTADO", operacion,
                6495, 3500, 9995, 5458, 1037);
    }

    @Test
    void laMismaClaveDeIdempotenciaDelMismoUsuarioNoPuedeDuplicarLaBoleta() {
        boletaAprobada("TP-OPERACION-0001", 1, "clave-cliente-0001");

        assertThatThrownBy(() -> boletaAprobada("TP-OPERACION-0002", 1, "clave-cliente-0001"))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta", Long.class)).isEqualTo(1L);
    }

    @Test
    void laMismaClaveEnOtroUsuarioSiEsPermitida() {
        boletaAprobada("TP-OPERACION-0001", 1, "clave-cliente-0001");
        boletaAprobada("TP-OPERACION-0002", 2, "clave-cliente-0001");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta", Long.class)).isEqualTo(2L);
    }

    @Test
    void elNumeroDeOperacionYLaReferenciaDeDescuentoSonUnicos() {
        boletaAprobada("TP-OPERACION-0001", 1, "clave-a-0001");

        assertThatThrownBy(() -> boletaAprobada("TP-OPERACION-0001", 2, "clave-b-0002"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void elNumeroDeBoletaPuedeAsignarseDespuesDeInsertarYEsUnico() {
        long primera = boletaAprobada("TP-OPERACION-0001", 1, "clave-a-0001");
        long segunda = boletaAprobada("TP-OPERACION-0002", 2, "clave-b-0002");

        // Dos boletas recién insertadas, sin número todavía (NULL): permitido.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta WHERE numero_boleta IS NULL", Long.class))
                .isEqualTo(2L);

        jdbc.update("UPDATE boleta SET numero_boleta = ? WHERE id = ?", "BOL-0001", primera);

        assertThatThrownBy(() -> jdbc.update("UPDATE boleta SET numero_boleta = ? WHERE id = ?", "BOL-0001", segunda))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void laBaseDeDatosRechazaMontosQueNoCuadran() {
        // total distinto de subtotal + despacho
        assertThatThrownBy(() -> insertarBoleta("TP-OPERACION-0001", 1, "clave-a-0001", "APROBADA", "DESCONTADO",
                "TP-OPERACION-0001", 6495, 3500, 9999, 5458, 1037))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_boleta_montos");

        // neto + iva distinto del subtotal de productos
        assertThatThrownBy(() -> insertarBoleta("TP-OPERACION-0002", 1, "clave-b-0002", "APROBADA", "DESCONTADO",
                "TP-OPERACION-0002", 6495, 3500, 9995, 5458, 1000))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_boleta_montos");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta", Long.class)).isZero();
    }

    @Test
    void laBaseDeDatosRechazaCombinacionesIncoherentesDeEstadoEInventario() {
        // Aprobada sin inventario descontado.
        assertThatThrownBy(() -> insertarBoleta("TP-OPERACION-0001", 1, "clave-a-0001", "APROBADA", "PENDIENTE",
                "TP-OPERACION-0001", 6495, 3500, 9995, 5458, 1037))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_boleta_aprobada_descontada");

        // Rechazada con el inventario todavía descontado.
        assertThatThrownBy(() -> insertarBoleta("TP-OPERACION-0002", 1, "clave-b-0002", "RECHAZADA", "DESCONTADO",
                "TP-OPERACION-0002", 6495, 3500, 9995, 5458, 1037))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_boleta_rechazada_sin_descuento");

        // Estado que no existe.
        assertThatThrownBy(() -> insertarBoleta("TP-OPERACION-0003", 1, "clave-c-0003", "ANULADA", "NO_APLICA",
                "TP-OPERACION-0003", 6495, 3500, 9995, 5458, 1037))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_boleta_estado");

        // Combinaciones válidas.
        insertarBoleta("TP-OPERACION-0004", 1, "clave-d-0004", "RECHAZADA", "NO_APLICA", null,
                6495, 3500, 9995, 5458, 1037);
        insertarBoleta("TP-OPERACION-0005", 1, "clave-e-0005", "PENDIENTE", "INCIERTO", "TP-OPERACION-0005",
                6495, 3500, 9995, 5458, 1037);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta", Long.class)).isEqualTo(2L);
    }

    private void insertarDetalle(long boletaId, long productoId, String talla, int cantidad, long precioFinal,
            long subtotal, String imagen, boolean imagenDisponible) {
        jdbc.update("INSERT INTO boleta_detalle (boleta_id, producto_id, codigo_producto, nombre_producto, imagen, "
                        + "imagen_disponible, talla, precio_original, descuento, precio_final, cantidad, subtotal) "
                        + "VALUES (?, ?, 'POL-001', 'Urban Red', ?, ?, ?, 12990, 50, ?, ?, ?)",
                boletaId, productoId, imagen, imagenDisponible, talla, precioFinal, cantidad, subtotal);
    }

    @Test
    void elDetalleConservaLaFotografiaDelProductoYSeBorraEnCascadaConLaBoleta() {
        long boleta = boletaAprobada("TP-OPERACION-0001", 1, "clave-a-0001");

        insertarDetalle(boleta, 1, "M", 1, 6495, 6495, "imagen1", true);
        jdbc.update("INSERT INTO boleta_evento (boleta_id, fecha, tipo, detalle) VALUES (?, UTC_TIMESTAMP(), "
                + "'CREADA', 'Boleta creada')", boleta);

        assertThat(jdbc.queryForObject("SELECT nombre_producto FROM boleta_detalle WHERE boleta_id = ?",
                String.class, boleta)).isEqualTo("Urban Red");

        jdbc.update("DELETE FROM boleta WHERE id = ?", boleta);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta_detalle", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta_evento", Long.class)).isZero();
    }

    @Test
    void laBaseDeDatosRechazaDetallesInvalidos() {
        long boleta = boletaAprobada("TP-OPERACION-0001", 1, "clave-a-0001");
        insertarDetalle(boleta, 1, "M", 1, 6495, 6495, null, false);

        // Mismo producto y talla dos veces en la misma boleta.
        assertThatThrownBy(() -> insertarDetalle(boleta, 1, "M", 1, 6495, 6495, null, false))
                .isInstanceOf(DuplicateKeyException.class);

        // Cantidad cero.
        assertThatThrownBy(() -> insertarDetalle(boleta, 2, "S", 0, 6495, 0, null, false))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_detalle_cantidad");

        // Subtotal que no es precio final por cantidad.
        assertThatThrownBy(() -> insertarDetalle(boleta, 3, "S", 2, 6495, 1, null, false))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_detalle_precios");

        // Imagen guardada pero marcada como no disponible.
        assertThatThrownBy(() -> insertarDetalle(boleta, 4, "S", 1, 6495, 6495, "imagen1", false))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_detalle_imagen");

        // El mismo producto en otra talla sí es válido.
        insertarDetalle(boleta, 1, "L", 1, 6495, 6495, null, false);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM boleta_detalle", Long.class)).isEqualTo(2L);
    }

    @Test
    void unDetalleNoPuedeApuntarAUnaBoletaInexistente() {
        assertThatThrownBy(() -> insertarDetalle(999_999L, 1, "M", 1, 6495, 6495, null, false))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_detalle_boleta");
    }
}
