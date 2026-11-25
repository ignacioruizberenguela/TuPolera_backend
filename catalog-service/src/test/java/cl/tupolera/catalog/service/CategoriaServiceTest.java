package cl.tupolera.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.tupolera.catalog.dto.AtributoDto;
import cl.tupolera.catalog.dto.CategoriaRequest;
import cl.tupolera.catalog.dto.CategoriaResponse;
import cl.tupolera.catalog.dto.MedidaDto;
import cl.tupolera.catalog.exception.ApiException;
import cl.tupolera.catalog.model.Categoria;
import cl.tupolera.catalog.repository.CategoriaRepository;
import cl.tupolera.catalog.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProductoRepository productoRepository;

    private CategoriaService service;

    @BeforeEach
    void preparar() {
        service = new CategoriaService(categoriaRepository, productoRepository, new CategoriaMapper());
    }

    private static int status(Throwable t) {
        return ((ApiException) t).getStatus().value();
    }

    private static CategoriaRequest pantalones(List<AtributoDto> atributos, List<MedidaDto> medidas) {
        return new CategoriaRequest(" Pantalones ", "Pantalones básicos", null, null,
                List.of("38", "40", "42"), atributos, medidas);
    }

    private static AtributoDto corte() {
        return new AtributoDto(null, "Tipo de corte", "seleccion", List.of("Recto", "Slim", "Ancho"), true);
    }

    private static MedidaDto cintura() {
        return new MedidaDto(null, "Contorno de cintura", "cm", true, true);
    }

    /* ------------------------- creación ------------------------- */

    @Test
    void creaUnaCategoriaNuevaSinModificarElCodigoGenerandoLosIdsDeAtributosYMedidas() {
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse respuesta = service.crear(pantalones(List.of(corte()), List.of(cintura())));

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        Categoria guardada = captor.getValue();

        assertThat(guardada.getNombre()).isEqualTo("Pantalones");
        assertThat(guardada.isActiva()).isTrue();
        assertThat(guardada.getTallas()).containsExactly("38", "40", "42");

        assertThat(respuesta.atributos()).hasSize(1);
        assertThat(respuesta.atributos().get(0).id()).isEqualTo("tipo_de_corte");
        assertThat(respuesta.atributos().get(0).opciones()).containsExactly("Recto", "Slim", "Ancho");
        assertThat(respuesta.atributos().get(0).obligatorio()).isTrue();
        assertThat(respuesta.medidas().get(0).id()).isEqualTo("contorno_de_cintura");
        assertThat(respuesta.medidas().get(0).unidad()).isEqualTo("cm");
    }

    @Test
    void rechazaUnNombreDeCategoriaDuplicado() {
        when(categoriaRepository.existsByNombreIgnoreCase("Pantalones")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(pantalones(List.of(), List.of())))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void unAtributoDeSeleccionNecesitaAlMenosDosOpciones() {
        AtributoDto pocasOpciones = new AtributoDto(null, "Tipo de corte", "seleccion", List.of("Recto"), false);

        assertThatThrownBy(() -> service.crear(pantalones(List.of(pocasOpciones), List.of())))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    @Test
    void rechazaTallasAtributosYMedidasRepetidos() {
        CategoriaRequest tallasRepetidas = new CategoriaRequest("Medias", null, null, null,
                List.of("M", "M"), List.of(), List.of());
        assertThatThrownBy(() -> service.crear(tallasRepetidas))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));

        assertThatThrownBy(() -> service.crear(pantalones(List.of(corte(), corte()), List.of())))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));

        assertThatThrownBy(() -> service.crear(pantalones(List.of(), List.of(cintura(), cintura()))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    @Test
    void unaMedidaDebeSolicitarseAlClienteORegistrarseEnElProducto() {
        MedidaDto inutil = new MedidaDto(null, "Medida inútil", "cm", false, false);

        assertThatThrownBy(() -> service.crear(pantalones(List.of(), List.of(inutil))))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    /* ------------------------- actualización ------------------------- */

    @Test
    void actualizaLosAtributosExistentesEnElLugarConservandoSuId() {
        Categoria poleras = TestData.poleras();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaRequest request = new CategoriaRequest("Poleras", "Básicas", null, null,
                List.of("S", "M"),
                List.of(new AtributoDto("tipo_manga", "Manga", "seleccion", List.of("Corta", "Larga", "3/4"), false)),
                List.of(new MedidaDto("contorno_pecho", "Pecho", "cm", true, true)));

        CategoriaResponse respuesta = service.actualizar(1L, request);

        // Misma clave -> se actualiza la fila existente (no se borra y recrea); la medida que no vino se elimina.
        assertThat(poleras.getAtributos()).hasSize(1);
        assertThat(poleras.getAtributos().get(0).getClave()).isEqualTo("tipo_manga");
        assertThat(poleras.getAtributos().get(0).getNombre()).isEqualTo("Manga");
        assertThat(poleras.getAtributos().get(0).getOpciones()).containsExactly("Corta", "Larga", "3/4");
        assertThat(respuesta.medidas()).extracting(MedidaDto::id).containsExactly("contorno_pecho");
        assertThat(respuesta.tallas()).containsExactly("S", "M");
    }

    @Test
    void noSePuedeDesactivarUnaCategoriaConProductosActivos() {
        Categoria poleras = TestData.poleras();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
        when(productoRepository.countByCategoriaIdAndActivoTrue(1L)).thenReturn(2L);

        CategoriaRequest desactivar = new CategoriaRequest("Poleras", null, null, false, List.of("S"), List.of(), List.of());

        assertThatThrownBy(() -> service.actualizar(1L, desactivar))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));
    }

    @Test
    void actualizarUnaCategoriaInexistenteResponde404() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(9L, pantalones(List.of(), List.of())))
                .satisfies(e -> assertThat(status(e)).isEqualTo(404));
    }

    /* ------------------------- eliminación ------------------------- */

    @Test
    void conProductosActivosNoSePuedeEliminar() {
        Categoria poleras = TestData.poleras();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
        when(productoRepository.countByCategoriaIdAndActivoTrue(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.eliminar(1L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));

        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }

    @Test
    void conSoloProductosInactivosSeDesactiva() {
        Categoria poleras = TestData.poleras();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
        when(productoRepository.countByCategoriaIdAndActivoTrue(1L)).thenReturn(0L);
        when(productoRepository.countByCategoriaId(1L)).thenReturn(3L);

        service.eliminar(1L);

        assertThat(poleras.isActiva()).isFalse();
        verify(categoriaRepository).save(poleras);
        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }

    @Test
    void sinProductosSeEliminaFisicamente() {
        Categoria poleras = TestData.poleras();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(poleras));
        when(productoRepository.countByCategoriaIdAndActivoTrue(1L)).thenReturn(0L);
        when(productoRepository.countByCategoriaId(1L)).thenReturn(0L);

        service.eliminar(1L);

        verify(categoriaRepository).delete(poleras);
    }

    /* ------------------------- consulta ------------------------- */

    @Test
    void unaCategoriaInactivaNoExisteParaElPublico() {
        Categoria inactiva = TestData.poleras();
        inactiva.setActiva(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> service.obtener(1L, false))
                .satisfies(e -> assertThat(status(e)).isEqualTo(404));

        assertThat(service.obtener(1L, true).activa()).isFalse();
    }

    @Test
    void elListadoPublicoSoloMuestraCategoriasActivas() {
        when(categoriaRepository.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(TestData.poleras()));
        when(categoriaRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(TestData.poleras(), TestData.poleras()));

        assertThat(service.listar(false)).hasSize(1);
        assertThat(service.listar(true)).hasSize(2);
    }
}
