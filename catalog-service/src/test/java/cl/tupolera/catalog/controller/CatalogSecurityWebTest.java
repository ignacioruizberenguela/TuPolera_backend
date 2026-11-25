package cl.tupolera.catalog.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import cl.tupolera.catalog.config.SecurityConfig;
import cl.tupolera.catalog.security.ApiSecurityHandlers;
import cl.tupolera.catalog.service.CategoriaService;
import cl.tupolera.catalog.service.ProductoService;

/** Roles, 401/403 y validaciones 400 de la capa web, sin base de datos. */
@WebMvcTest(controllers = { ProductoController.class, CategoriaController.class })
@Import({ SecurityConfig.class, ApiSecurityHandlers.class })
class CatalogSecurityWebTest {

    private static final String PRODUCTO_VALIDO = """
            {"codigo":"POL-010","nombre":"Polera","precio":9990,"categoriaId":1,
             "tallas":{"M":{"stock":5,"medidas":{"contorno_pecho":100,"largo_prenda":68}}},
             "atributos":{"tipo_manga":"Corta"}}
            """;

    private static final String CATEGORIA_VALIDA = """
            {"nombre":"Pantalones","descripcion":"Pantalones básicos","tallas":["38","40"],
             "atributos":[{"nombre":"Tipo de corte","tipo":"seleccion","opciones":["Recto","Slim"],"obligatorio":true}],
             "medidas":[{"nombre":"Contorno de cintura","unidad":"cm","solicitarCliente":true,"registrarProducto":true}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ProductoService productoService;
    @MockitoBean private CategoriaService categoriaService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    /* ------------------------- lectura pública ------------------------- */

    @Test
    void productosYCategoriasSePuedenConsultarSinIniciarSesion() throws Exception {
        mockMvc.perform(get("/api/v1/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/productos/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/productos/categoria/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categorias/1")).andExpect(status().isOk());
    }

    @Test
    void soloElPersonalPuedePedirLosProductosInactivos() throws Exception {
        mockMvc.perform(get("/api/v1/productos?incluirInactivos=true")).andExpect(status().isOk());
        verify(productoService).listar(false);

        mockMvc.perform(get("/api/v1/productos?incluirInactivos=true").with(como("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/productos?incluirInactivos=true").with(como("VENDEDOR")))
                .andExpect(status().isOk());
        verify(productoService).listar(true);
    }

    /* ------------------------- productos críticos ------------------------- */

    @Test
    void productosCriticosSoloParaAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/productos/criticos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/productos/criticos").with(como("CLIENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/productos/criticos").with(como("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/productos/criticos").with(como("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    /* ------------------------- escritura de productos ------------------------- */

    @Test
    void crearProductoSinTokenResponde401() throws Exception {
        mockMvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientesYVendedoresNoPuedenModificarProductos() throws Exception {
        for (String rol : new String[] { "CLIENTE", "VENDEDOR" }) {
            mockMvc.perform(post("/api/v1/productos").with(como(rol))
                    .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/productos/1").with(como(rol))
                    .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/api/v1/productos/1").with(como(rol)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elAdministradorPuedeCrearActualizarYDesactivarProductos() throws Exception {
        mockMvc.perform(post("/api/v1/productos").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/productos/1").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/productos/1").with(como("ADMINISTRADOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    void productoConDatosInvalidosResponde400ConDetalle() throws Exception {
        String invalido = """
                {"codigo":"ab","nombre":"","precio":-1,"descuento":150,"categoriaId":null,
                 "stockCritico":-3,"tallas":{}}
                """;

        mockMvc.perform(post("/api/v1/productos").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.detalles[?(@.campo=='codigo')]").exists())
                .andExpect(jsonPath("$.detalles[?(@.campo=='precio')]").exists())
                .andExpect(jsonPath("$.detalles[?(@.campo=='tallas')]").exists());
    }

    @Test
    void elStockDeUnaTallaNoPuedeSerNegativo() throws Exception {
        String stockNegativo = """
                {"codigo":"POL-011","nombre":"Polera","precio":9990,"categoriaId":1,
                 "tallas":{"M":{"stock":-4,"medidas":{}}}}
                """;

        mockMvc.perform(post("/api/v1/productos").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(stockNegativo))
                .andExpect(status().isBadRequest());
    }

    /* ------------------------- categorías ------------------------- */

    @Test
    void soloElAdministradorModificaCategorias() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                .andExpect(status().isUnauthorized());

        for (String rol : new String[] { "CLIENTE", "VENDEDOR" }) {
            mockMvc.perform(post("/api/v1/categorias").with(como(rol))
                    .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/categorias/1").with(como(rol))
                    .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/categorias/1").with(como(rol)))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/v1/categorias").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/categorias/1").with(como("ADMINISTRADOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    void categoriaSinNombreNiTallasResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/categorias").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"tallas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void unAtributoConTipoInvalidoResponde400() throws Exception {
        String tipoInvalido = """
                {"nombre":"Medias","tallas":["Única"],
                 "atributos":[{"nombre":"Material","tipo":"inventado"}]}
                """;

        mockMvc.perform(post("/api/v1/categorias").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(tipoInvalido))
                .andExpect(status().isBadRequest());
    }
}
