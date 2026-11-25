package cl.tupolera.catalog.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import cl.tupolera.catalog.dto.AtributoDto;
import cl.tupolera.catalog.dto.CategoriaResponse;
import cl.tupolera.catalog.dto.MedidaDto;
import cl.tupolera.catalog.model.Categoria;

@Component
public class CategoriaMapper {

    public CategoriaResponse toResponse(Categoria c) {
        List<AtributoDto> atributos = c.getAtributos().stream()
                .map(a -> new AtributoDto(a.getClave(), a.getNombre(), a.getTipo(),
                        new ArrayList<>(a.getOpciones()), a.isObligatorio()))
                .toList();

        List<MedidaDto> medidas = c.getMedidas().stream()
                .map(m -> new MedidaDto(m.getClave(), m.getNombre(), m.getUnidad(),
                        m.isSolicitarCliente(), m.isRegistrarProducto()))
                .toList();

        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getImagen(),
                c.isActiva(), new ArrayList<>(c.getTallas()), atributos, medidas);
    }
}
