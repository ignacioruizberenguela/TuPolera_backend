package cl.tupolera.catalog.integracion;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

/**
 * Esta prueba SIEMPRE se ejecuta y solo informa si Docker está disponible, para que quede claro en la salida de
 * Maven si las pruebas de concurrencia (InventarioIntegracionTest) pudieron correr o se omitieron.
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
                ? "Docker DISPONIBLE: se ejecutaran las pruebas de integracion con MySQL real (atomicidad y concurrencia)."
                : "Docker NO disponible: las pruebas de integracion con MySQL real se OMITIRAN. "
                        + "BUILD SUCCESS no validara la atomicidad ni la concurrencia." + causa));
    }
}
