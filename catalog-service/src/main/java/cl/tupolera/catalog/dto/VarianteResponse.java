package cl.tupolera.catalog.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VarianteResponse(int stock, Map<String, BigDecimal> medidas) {
}
