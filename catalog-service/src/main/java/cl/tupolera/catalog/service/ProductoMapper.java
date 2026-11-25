package cl.tupolera.catalog.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cl.tupolera.catalog.dto.ProductoResponse;
import cl.tupolera.catalog.dto.VarianteResponse;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.model.CategoriaAtributo;
import cl.tupolera.catalog.model.Producto;
import cl.tupolera.catalog.model.ProductoVariante;

@Component
public class ProductoMapper {

    /** Precio con el descuento aplicado, redondeado al peso (misma fórmula que el frontend). */
    public static int precioFinal(int precio, int descuento) {
        return (int) Math.round(precio - (precio * descuento / 100.0));
    }

    /** Una talla es crítica cuando su stock es menor o igual al stock crítico del producto. */
    public static List<String> tallasCriticas(Producto p) {
        List<String> criticas = new ArrayList<>();
        for (ProductoVariante v : p.getVariantes()) {
            if (v.getStock() <= p.getStockCritico()) {
                criticas.add(v.getTalla());
            }
        }
        return criticas;
    }

    public ProductoResponse toResponse(Producto p) {
        Categoria categoria = p.getCategoria();
        List<String> criticas = tallasCriticas(p);

        return new ProductoResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getDescuento(),
                precioFinal(p.getPrecio(), p.getDescuento()),
                p.getColor(),
                categoria.getId(),
                categoria.getNombre(),
                p.getImagen(),
                tallas(p, categoria),
                atributos(p, categoria),
                p.getStockTotal(),
                p.getStockCritico(),
                !criticas.isEmpty(),
                criticas,
                p.isActivo(),
                p.getFechaCreacion(),
                p.getFechaActualizacion());
    }

    /** Tallas en el mismo orden que define la categoría. */
    private Map<String, VarianteResponse> tallas(Producto p, Categoria categoria) {
        Map<String, VarianteResponse> resultado = new LinkedHashMap<>();

        List<ProductoVariante> ordenadas = new ArrayList<>(p.getVariantes());
        ordenadas.sort((a, b) -> Integer.compare(posicion(categoria, a.getTalla()), posicion(categoria, b.getTalla())));

        for (ProductoVariante v : ordenadas) {
            resultado.put(v.getTalla(), new VarianteResponse(v.getStock(), new LinkedHashMap<>(v.getMedidas())));
        }
        return resultado;
    }

    private static int posicion(Categoria categoria, String talla) {
        int indice = categoria.getTallas().indexOf(talla);
        return indice == -1 ? Integer.MAX_VALUE : indice;
    }

    /** Devuelve cada valor con el tipo que define la categoría (booleano, número o texto). */
    private Map<String, Object> atributos(Producto p, Categoria categoria) {
        Map<String, Object> resultado = new LinkedHashMap<>();

        for (Map.Entry<String, String> entrada : p.getAtributos().entrySet()) {
            String tipo = categoria.getAtributos().stream()
                    .filter(a -> a.getClave().equals(entrada.getKey()))
                    .map(CategoriaAtributo::getTipo)
                    .findFirst()
                    .orElse(CategoriaAtributo.TEXTO);

            String valor = entrada.getValue();

            if (CategoriaAtributo.BOOLEANO.equals(tipo)) {
                resultado.put(entrada.getKey(), Boolean.parseBoolean(valor));
            } else if (CategoriaAtributo.NUMERO.equals(tipo)) {
                resultado.put(entrada.getKey(), new BigDecimal(valor));
            } else {
                resultado.put(entrada.getKey(), valor);
            }
        }
        return resultado;
    }
}
