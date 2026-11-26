package cl.tupolera.sales.integracion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Al terminar la ejecución imprime cuántas pruebas con la etiqueta "integracion-docker" se ejecutaron y cuántas se
 * omitieron (por ejemplo por falta de Docker). Así un BUILD SUCCESS no se puede confundir con "las pruebas con MySQL real
 * se ejecutaron". También escribe el resumen en target/resumen-integracion-docker.txt.
 *
 * Se registra automáticamente con ServiceLoader (META-INF/services/org.junit.platform.launcher.TestExecutionListener).
 */
public class ResumenIntegracionDockerListener implements TestExecutionListener {

    private static final String ETIQUETA = "integracion-docker";

    private final AtomicInteger ejecutadas = new AtomicInteger();
    private final AtomicInteger aprobadas = new AtomicInteger();
    private final AtomicInteger omitidas = new AtomicInteger();
    private final AtomicReference<String> motivo = new AtomicReference<>("");
    private volatile TestPlan plan;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.plan = testPlan;
    }

    @Override
    public void executionSkipped(TestIdentifier identificador, String razon) {
        if (!esDeIntegracion(identificador)) {
            return;
        }

        // Si se omite la clase completa, se cuentan todas sus pruebas.
        int cantidad = identificador.isTest() ? 1 : (int) plan.getDescendants(identificador).stream()
                .filter(TestIdentifier::isTest).count();

        omitidas.addAndGet(cantidad);
        motivo.compareAndSet("", razon == null ? "" : razon);
    }

    @Override
    public void executionFinished(TestIdentifier identificador, TestExecutionResult resultado) {
        if (identificador.isTest() && esDeIntegracion(identificador)) {
            ejecutadas.incrementAndGet();
            if (resultado.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                aprobadas.incrementAndGet();
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (ejecutadas.get() == 0 && omitidas.get() == 0) {
            return;   // no se seleccionó ninguna prueba de integración en esta ejecución
        }

        StringBuilder texto = new StringBuilder();
        texto.append("\n==================================================================\n");
        texto.append(" PRUEBAS DE INTEGRACION CON DOCKER (MySQL real: esquema, configuracion y restricciones)\n");
        texto.append(" Ejecutadas: ").append(ejecutadas.get())
                .append("   Aprobadas: ").append(aprobadas.get())
                .append("   OMITIDAS: ").append(omitidas.get()).append('\n');

        if (omitidas.get() > 0) {
            texto.append(" ATENCION: ").append(omitidas.get()).append(" prueba(s) NO se ejecutaron.\n");
            texto.append(" Motivo: ").append(motivo.get()).append('\n');
            texto.append(" BUILD SUCCESS NO valida el esquema ni las restricciones reales de MySQL. Inicia Docker Desktop\n");
            texto.append(" y vuelve a ejecutar las pruebas.\n");
            texto.append(" Si Docker Desktop SI esta en ejecucion y aun asi se omiten, puede ser una incompatibilidad\n");
            texto.append(" entre la version de Testcontainers y la API de tu Docker (ver README, seccion Pruebas).\n");
        } else if (ejecutadas.get() == aprobadas.get()) {
            texto.append(" Todas las pruebas de integracion con Docker se ejecutaron y pasaron.\n");
        } else {
            texto.append(" Hay pruebas de integracion con Docker que FALLARON.\n");
        }
        texto.append("==================================================================\n");

        System.out.println(texto);

        try {
            Path destino = Path.of("target", "resumen-integracion-docker.txt");
            Files.createDirectories(destino.getParent());
            Files.writeString(destino, texto.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo escribir el resumen de integracion: " + e.getMessage());
        }
    }

    private static boolean esDeIntegracion(TestIdentifier identificador) {
        return identificador.getTags().contains(TestTag.create(ETIQUETA));
    }
}
