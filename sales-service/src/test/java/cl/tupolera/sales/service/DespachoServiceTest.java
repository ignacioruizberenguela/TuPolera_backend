package cl.tupolera.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.tupolera.sales.dto.ConfiguracionDespachoRequest;
import cl.tupolera.sales.dto.ConfiguracionDespachoResponse;
import cl.tupolera.sales.dto.CotizacionResponse;
import cl.tupolera.sales.dto.DestinoRequest;
import cl.tupolera.sales.exception.ApiException;
import cl.tupolera.sales.model.ConfiguracionDespacho;
import cl.tupolera.sales.repository.ConfiguracionDespachoRepository;

@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock private ConfiguracionDespachoRepository repository;

    private DespachoService service;
    private ConfiguracionDespacho configuracion;

    @BeforeEach
    void preparar() {
        service = new DespachoService(repository);
        configuracion = ConfiguracionDespacho.porDefecto();
    }

    private void conConfiguracion() {
        when(repository.findById(ConfiguracionDespacho.ID_UNICO)).thenReturn(Optional.of(configuracion));
    }

    private static int status(Throwable t) {
        return ((ApiException) t).getStatus().value();
    }

    private static DestinoRequest destino(String direccion, Double latitud, Double longitud) {
        return new DestinoRequest(direccion, latitud, longitud);
    }

    /* ----------------------------- configuración ----------------------------- */

    @Test
    void siNoExisteLaConfiguracionSeCreaConLosValoresIniciales() {
        when(repository.findById(ConfiguracionDespacho.ID_UNICO)).thenReturn(Optional.empty());
        when(repository.save(any(ConfiguracionDespacho.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionDespachoResponse r = service.consultarConfiguracion();

        assertThat(r.nombreBodega()).isEqualTo("Bodega de prueba Tu Polera");
        assertThat(r.latitudBodega()).isEqualTo(-33.4372);
        assertThat(r.longitudBodega()).isEqualTo(-70.6506);
        assertThat(r.precioBase()).isEqualTo(2990);
        assertThat(r.costoPorKm()).isEqualTo(150);
        assertThat(r.tarifaMinima()).isEqualTo(2990);
        assertThat(r.tarifaMaxima()).isEqualTo(15000);
        assertThat(r.esUbicacionPrueba()).isTrue();
        verify(repository).save(any(ConfiguracionDespacho.class));
    }

    private static ConfiguracionDespachoRequest nuevaConfiguracion(Double latitud, Double longitud, Long minima,
            Long maxima) {
        return new ConfiguracionDespachoRequest("  Bodega   Central ", " Av. Siempre Viva 742, Santiago ",
                latitud, longitud, 3500L, 200L, minima, maxima, false);
    }

    @Test
    void actualizaLaConfiguracionYRegistraQuienLaCambio() {
        conConfiguracion();
        when(repository.save(any(ConfiguracionDespacho.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionDespachoResponse r = service.actualizarConfiguracion(
                nuevaConfiguracion(-33.45, -70.66, 3000L, 18000L), 7L);

        assertThat(r.nombreBodega()).isEqualTo("Bodega Central");
        assertThat(r.direccionBodega()).isEqualTo("Av. Siempre Viva 742, Santiago");
        assertThat(r.latitudBodega()).isEqualTo(-33.45);
        assertThat(r.precioBase()).isEqualTo(3500);
        assertThat(r.costoPorKm()).isEqualTo(200);
        assertThat(r.tarifaMinima()).isEqualTo(3000);
        assertThat(r.tarifaMaxima()).isEqualTo(18000);
        assertThat(r.esUbicacionPrueba()).isFalse();
        assertThat(configuracion.getActualizadoPor()).isEqualTo(7L);
    }

    @Test
    void lasTarifasMinimaYMaximaSonOpcionales() {
        conConfiguracion();
        when(repository.save(any(ConfiguracionDespacho.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionDespachoResponse r = service.actualizarConfiguracion(
                nuevaConfiguracion(-33.45, -70.66, null, null), 7L);

        assertThat(r.tarifaMinima()).isNull();
        assertThat(r.tarifaMaxima()).isNull();
    }

    @Test
    void rechazaUnaTarifaMaximaMenorQueLaMinima() {
        assertThatThrownBy(() -> service.actualizarConfiguracion(
                nuevaConfiguracion(-33.45, -70.66, 5000L, 4000L), 7L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));

        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnaBodegaFueraDeChile() {
        assertThatThrownBy(() -> service.actualizarConfiguracion(
                nuevaConfiguracion(-34.6037, -58.3816, null, null), 7L))   // Buenos Aires
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));

        verify(repository, never()).save(any());
    }

    @Test
    void rechazaNombreODireccionEnBlanco() {
        ConfiguracionDespachoRequest enBlanco = new ConfiguracionDespachoRequest("   ", "Calle 1", -33.45, -70.66,
                3500L, 200L, null, null, true);

        assertThatThrownBy(() -> service.actualizarConfiguracion(enBlanco, 7L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    /* ----------------------------- cotización ----------------------------- */

    @Test
    void conElMismoPuntoDeLaBodegaLaDistanciaEsCeroYSeCobraElPrecioBase() {
        conConfiguracion();

        CotizacionResponse r = service.cotizar(destino("Bodega", -33.4372, -70.6506));

        assertThat(r.distanciaKm()).isZero();
        assertThat(r.precio()).isEqualTo(2990);
        assertThat(r.tipoCalculo()).isEqualTo("HAVERSINE_LINEA_RECTA");
    }

    @Test
    void cotizaDistintasDistanciasConLaConfiguracionInicial() {
        conConfiguracion();

        CotizacionResponse providencia = service.cotizar(destino("Providencia", -33.4251, -70.6167));
        assertThat(providencia.distanciaKm()).isCloseTo(3.421, within(0.001));
        assertThat(providencia.precio()).isEqualTo(3500);

        CotizacionResponse valparaiso = service.cotizar(destino("Valparaíso", -33.0472, -71.6127));
        assertThat(valparaiso.distanciaKm()).isCloseTo(99.429, within(0.001));
        assertThat(valparaiso.precio()).isEqualTo(15000);            // tarifa máxima

        CotizacionResponse arica = service.cotizar(destino("Arica", -18.4783, -70.3126));
        assertThat(arica.precio()).isEqualTo(15000);
    }

    @Test
    void laRespuestaIncluyeElDestinoYLaBodegaDeOrigen() {
        conConfiguracion();

        CotizacionResponse r = service.cotizar(destino("  Av. Providencia   1234, Providencia ", -33.4251, -70.6167));

        assertThat(r.direccion()).isEqualTo("Av. Providencia 1234, Providencia");   // espacios normalizados
        assertThat(r.latitud()).isEqualTo(-33.4251);
        assertThat(r.longitud()).isEqualTo(-70.6167);
        assertThat(r.origen().nombre()).isEqualTo("Bodega de prueba Tu Polera");
        assertThat(r.origen().esUbicacionPrueba()).isTrue();
    }

    @Test
    void usaLaBodegaYLasTarifasConfiguradasNoValoresFijos() {
        configuracion.setLatitudBodega(-33.0472);          // la bodega ahora está en Valparaíso
        configuracion.setLongitudBodega(-71.6127);
        configuracion.setPrecioBase(1000);
        configuracion.setCostoPorKm(100);
        configuracion.setTarifaMinima(null);
        configuracion.setTarifaMaxima(null);
        conConfiguracion();

        CotizacionResponse aqui = service.cotizar(destino("En la bodega", -33.0472, -71.6127));
        assertThat(aqui.distanciaKm()).isZero();
        assertThat(aqui.precio()).isEqualTo(1000);

        // 99,429 km desde Valparaíso hasta Santiago: 1000 + 100 * 99,429 = 10942,9 -> 10940
        CotizacionResponse santiago = service.cotizar(destino("Santiago", -33.4372, -70.6506));
        assertThat(santiago.precio()).isEqualTo(10940);
    }

    @Test
    void rechazaUnaDireccionVaciaOSoloEspacios() {
        conConfiguracion();

        for (String direccion : new String[] { "", "   ", null }) {
            assertThatThrownBy(() -> service.cotizar(destino(direccion, -33.4, -70.6)))
                    .satisfies(e -> assertThat(status(e)).isEqualTo(400));
        }
    }

    @Test
    void rechazaCoordenadasInvalidas() {
        conConfiguracion();

        Double[][] invalidas = {
                { 91.0, -70.0 }, { -91.0, -70.0 }, { -33.0, 181.0 }, { -33.0, -181.0 },
                { Double.NaN, -70.0 }, { -33.0, Double.NaN },
                { Double.POSITIVE_INFINITY, -70.0 }, { -33.0, Double.NEGATIVE_INFINITY }
        };

        for (Double[] c : invalidas) {
            assertThatThrownBy(() -> service.cotizar(destino("Calle 1", c[0], c[1])))
                    .as("lat=%s lon=%s", c[0], c[1])
                    .satisfies(e -> assertThat(status(e)).isEqualTo(400));
        }
    }

    @Test
    void rechazaCoordenadasFaltantes() {
        conConfiguracion();

        assertThatThrownBy(() -> service.cotizar(destino("Calle 1", null, -70.6)))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
        assertThatThrownBy(() -> service.cotizar(destino("Calle 1", -33.4, null)))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    @Test
    void rechazaDestinosFueraDeChile() {
        conConfiguracion();

        assertThatThrownBy(() -> service.cotizar(destino("Buenos Aires", -34.6037, -58.3816)))
                .satisfies(e -> {
                    assertThat(status(e)).isEqualTo(400);
                    assertThat(e.getMessage()).contains("Chile");
                });
        assertThatThrownBy(() -> service.cotizar(destino("Madrid", 40.4168, -3.7038)))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    @Test
    void laCotizacionNoModificaLaConfiguracion() {
        conConfiguracion();

        service.cotizar(destino("Providencia", -33.4251, -70.6167));

        verify(repository, never()).save(any());
    }
}
