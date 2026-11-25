package cl.tupolera.catalog.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;

import cl.tupolera.catalog.dto.ProductoRequest;
import cl.tupolera.catalog.dto.VarianteRequest;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.model.CategoriaAtributo;
import cl.tupolera.catalog.model.CategoriaMedida;
import cl.tupolera.catalog.model.Producto;
import cl.tupolera.catalog.model.ProductoVariante;

/** Datos de prueba compartidos por las pruebas del servicio. */
final class TestData {

    private TestData() {
    }

    /** Categoría "Poleras": tallas S, M, L; atributo obligatorio tipo_manga; medidas de pecho y largo. */
    static Categoria poleras() {
        Categoria c = new Categoria();
        ReflectionTestUtils.setField(c, "id", 1L);
        c.setNombre("Poleras");
        c.setActiva(true);
        c.getTallas().addAll(List.of("S", "M", "L"));

        CategoriaAtributo manga = new CategoriaAtributo(c, "tipo_manga");
        manga.setNombre("Tipo de manga");
        manga.setTipo(CategoriaAtributo.SELECCION);
        manga.setObligatorio(true);
        manga.getOpciones().addAll(List.of("Corta", "Larga"));
        c.getAtributos().add(manga);

        CategoriaMedida pecho = new CategoriaMedida(c, "contorno_pecho");
        pecho.setNombre("Contorno de pecho");
        pecho.setUnidad("cm");
        pecho.setSolicitarCliente(true);
        pecho.setRegistrarProducto(true);
        c.getMedidas().add(pecho);

        CategoriaMedida largo = new CategoriaMedida(c, "largo_prenda");
        largo.setNombre("Largo de prenda");
        largo.setUnidad("cm");
        largo.setSolicitarCliente(false);
        largo.setRegistrarProducto(true);
        c.getMedidas().add(largo);

        return c;
    }

    static VarianteRequest variante(int stock, double pecho, double largo) {
        Map<String, BigDecimal> medidas = new LinkedHashMap<>();
        medidas.put("contorno_pecho", BigDecimal.valueOf(pecho));
        medidas.put("largo_prenda", BigDecimal.valueOf(largo));
        return new VarianteRequest(stock, medidas);
    }

    static ProductoRequest solicitud(Map<String, VarianteRequest> tallas, Map<String, Object> atributos) {
        return new ProductoRequest(" POL-010 ", " Urban Red ", "Polera liviana", 12990, null, "rojo", 1L,
                "imagen1", null, null, tallas, atributos);
    }

    static Map<String, VarianteRequest> tallasValidas() {
        Map<String, VarianteRequest> tallas = new LinkedHashMap<>();
        tallas.put("S", variante(5, 96, 66));
        tallas.put("M", variante(10, 102, 68));
        return tallas;
    }

    static Producto producto(Categoria categoria, long id, int stockCritico, int... stocks) {
        Producto p = new Producto();
        ReflectionTestUtils.setField(p, "id", id);
        p.setCodigo("POL-" + id);
        p.setNombre("Producto " + id);
        p.setPrecio(10000);
        p.setCategoria(categoria);
        p.setStockCritico(stockCritico);
        p.setActivo(true);

        String[] tallas = { "S", "M", "L" };
        for (int i = 0; i < stocks.length; i++) {
            ProductoVariante v = new ProductoVariante(p, tallas[i]);
            v.setStock(stocks[i]);
            p.getVariantes().add(v);
        }
        return p;
    }
}
