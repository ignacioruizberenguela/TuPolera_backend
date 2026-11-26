package cl.tupolera.sales.util;

/**
 * Cálculos de dinero en pesos chilenos (CLP). Todos los montos son enteros (long): el CLP no usa decimales.
 * Los precios del catálogo YA INCLUYEN el IVA del 19%.
 */
public final class Dinero {

    private Dinero() {
    }

    /**
     * Precio con el descuento aplicado. Es exactamente la fórmula del frontend y del catálogo:
     * Math.round(precio - (precio * descuento / 100.0)).
     */
    public static long precioFinal(long precio, int descuento) {
        return Math.round(precio - (precio * descuento / 100.0));
    }

    /**
     * Neto de un monto bruto (con IVA 19% incluido): bruto / 1,19 redondeado al peso más cercano.
     * Se calcula solo con enteros: (200 * bruto + 119) / 238 equivale a redondear bruto * 100 / 119.
     */
    public static long neto(long bruto) {
        return (200 * bruto + 119) / 238;
    }

    /** IVA contenido en un monto bruto: lo que queda al quitar el neto. Siempre neto + iva = bruto. */
    public static long iva(long bruto) {
        return bruto - neto(bruto);
    }
}
