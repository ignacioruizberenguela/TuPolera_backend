package cl.tupolera.sales.integracion;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

/**
 * Esta prueba SIEMPRE se ejecuta y solo informa si Docker está disponible, para que quede claro en la salida de
 * Maven si las pruebas de integracion con MySQL real (SalesDbIntegracionTest) pudieron correr o se omitieron.
 */
class EstadoDockerTest {

    @Test
    void informaSiDockerEstaDisponible() {
        boolean disponible;
        String causa = "";
        try {
            disponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            disponible = false;
            causa = " Causa: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        System.out.println("[ESTADO DOCKER] "
                + (disponible
                ? "Docker DISPONIBLE: se ejecutaran las pruebas de integracion con MySQL real (esquema, configuracion y restricciones)."
                : "Docker NO disponible: las pruebas de integracion con MySQL real se OMITIRAN. "
                        + "BUILD SUCCESS no validara el esquema ni las restricciones reales de MySQL." + causa));
    }
}
