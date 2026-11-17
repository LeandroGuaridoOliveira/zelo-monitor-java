package br.com.fiap.zelo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Login com credenciais válidas emite JWT")
    void testLoginSucesso() throws Exception {
        String loginPayload = "{\"email\":\"maria@zelo.com\",\"senha\":\"123456\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.papel").value("IDOSO"));
    }

    @Test
    @DisplayName("Acesso com papel IDOSO a rota exclusiva de ADMIN retorna 403 ACESSO_NEGADO")
    void testAcessoNegadoPorPapel() throws Exception {
        String loginMaria = "{\"email\":\"maria@zelo.com\",\"senha\":\"123456\"}";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginMaria))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(res.getResponse().getContentAsString());
        String tokenMaria = jsonNode.path("token").asText();

        String payloadNovaCasa = "{\"idosoId\":\"00000000-0000-0000-0000-000000000099\",\"nomeIdoso\":\"Teste\",\"endereco\":\"Rua 1\"}";

        mockMvc.perform(post("/api/v1/casas")
                        .header("Authorization", "Bearer " + tokenMaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadNovaCasa))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Validação Bean Validation retorna formato padrão com codigo VALIDACAO_ERRO")
    void testValidacaoErroPadrao() throws Exception {
        String loginAdmin = "{\"email\":\"admin@zelo.com\",\"senha\":\"123456\"}";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(res.getResponse().getContentAsString());
        String tokenAdmin = jsonNode.path("token").asText();

        // Payload sem campos obrigatórios
        String payloadInvalido = "{}";

        mockMvc.perform(post("/api/v1/casas")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_ERRO"))
                .andExpect(jsonPath("$.mensagem", containsString("obrigatório")));
    }

    @Test
    @DisplayName("Consulta paginada de casas retorna lista com paginação")
    void testListagemCasasPaginada() throws Exception {
        String loginAdmin = "{\"email\":\"admin@zelo.com\",\"senha\":\"123456\"}";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(res.getResponse().getContentAsString());
        String tokenAdmin = jsonNode.path("token").asText();

        mockMvc.perform(get("/api/v1/casas?page=0&size=5")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.pageable", notNullValue()));
    }
}
