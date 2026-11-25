package cl.tupolera.auth.util;

/**
 * Utilidades para el RUN/RUT chileno. Se almacena sin puntos ni guion (por ejemplo 123456785).
 */
public final class RunUtil {

    private RunUtil() {
    }

    /** Quita puntos, guion y espacios y pasa a mayúsculas. */
    public static String normalizar(String run) {
        if (run == null) {
            return null;
        }
        return run.replaceAll("[.\\-\\s]", "").toUpperCase();
    }

    /** Valida formato (7 u 8 dígitos + verificador) y dígito verificador (módulo 11). */
    public static boolean esValido(String run) {
        String limpio = normalizar(run);

        if (limpio == null || !limpio.matches("^\\d{7,8}[0-9K]$")) {
            return false;
        }

        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char digitoVerificador = limpio.charAt(limpio.length() - 1);

        int suma = 0;
        int multiplo = 2;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplo;
            multiplo = multiplo < 7 ? multiplo + 1 : 2;
        }

        int resultado = 11 - (suma % 11);
        char esperado;

        if (resultado == 11) {
            esperado = '0';
        } else if (resultado == 10) {
            esperado = 'K';
        } else {
            esperado = Character.forDigit(resultado, 10);
        }

        return digitoVerificador == esperado;
    }
}
