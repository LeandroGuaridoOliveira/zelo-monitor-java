package br.com.fiap.zelo.service;

import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import br.com.fiap.zelo.enums.OrigemClassificacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CheckinAiServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Classificação bem-sucedida via OpenAiChatClient (LLM)")
    void classificacaoComMockLLM() {
        OpenAiChatClient mockClient = Mockito.mock(OpenAiChatClient.class);
        String jsonLlm = "{\"classificacao\":\"ESTOU_BEM\",\"resposta\":\"Que bom que você está bem, Dona Maria!\",\"motivo\":\"Idoso declarou tranquilidade\"}";
        Generation generation = new Generation(jsonLlm);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(mockClient.call(any(Prompt.class))).thenReturn(chatResponse);

        CheckinAiService service = new CheckinAiService(
                Optional.of(mockClient),
                "sk-teste-chave-valida-1234567890",
                objectMapper
        );

        CheckinAiService.ResultadoCheckin resultado = service.classificarTextoIdoso(
                "Acordei agora há pouco, assistindo televisão",
                "Sem atividade matinal"
        );

        assertEquals(ClassificacaoCheckin.ESTOU_BEM, resultado.classificacao());
        assertEquals(OrigemClassificacao.LLM, resultado.origem());
        assertNotNull(resultado.respostaParaIdoso());
    }

    @Test
    @DisplayName("Guardrail de segurança: forçar PRECISO_AJUDA se texto contiver palavra de risco mesmo se LLM disser ESTOU_BEM")
    void guardrailSobrescreveLLM() {
        OpenAiChatClient mockClient = Mockito.mock(OpenAiChatClient.class);
        String jsonLlm = "{\"classificacao\":\"ESTOU_BEM\",\"resposta\":\"Tudo certo!\",\"motivo\":\"Parece bem\"}";
        Generation generation = new Generation(jsonLlm);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(mockClient.call(any(Prompt.class))).thenReturn(chatResponse);

        CheckinAiService service = new CheckinAiService(
                Optional.of(mockClient),
                "sk-teste-chave-valida-1234567890",
                objectMapper
        );

        // Texto com palavra de risco 'caí' e 'dor'
        CheckinAiService.ResultadoCheckin resultado = service.classificarTextoIdoso(
                "Eu caí no tapete mas já estou bem com um pouco de dor",
                "Tempo excessivo no banheiro"
        );

        assertEquals(ClassificacaoCheckin.PRECISO_AJUDA, resultado.classificacao(), "Guardrail deve barrar ESTOU_BEM para texto com relato de queda");
    }

    @Test
    @DisplayName("Fallback por palavras-chave quando LLM falha com exceção ou chave ausente")
    void fallbackPalavrasChave() {
        CheckinAiService service = new CheckinAiService(
                Optional.empty(),
                "chave-nao-configurada",
                objectMapper
        );

        // Caso 1: Risco ("tonta", "não consigo")
        CheckinAiService.ResultadoCheckin resRisco = service.classificarTextoIdoso(
                "estou meio tonta, não consigo levantar",
                "Banheiro prolongado"
        );
        assertEquals(ClassificacaoCheckin.PRECISO_AJUDA, resRisco.classificacao());
        assertEquals(OrigemClassificacao.PALAVRAS_CHAVE, resRisco.origem());
        assertTrue(resRisco.respostaParaIdoso().contains("Ana"));

        // Caso 2: Bem-estar ("estou bem", "só dormi")
        CheckinAiService.ResultadoCheckin resBem = service.classificarTextoIdoso(
                "estou bem, só dormi mais um pouco",
                "Sem atividade matinal"
        );
        assertEquals(ClassificacaoCheckin.ESTOU_BEM, resBem.classificacao());
        assertEquals(OrigemClassificacao.PALAVRAS_CHAVE, resBem.origem());

        // Caso 3: Ambíguo / Incerto
        CheckinAiService.ResultadoCheckin resIncerto = service.classificarTextoIdoso(
                "quem está aí na porta?",
                "Alerta"
        );
        assertEquals(ClassificacaoCheckin.INCERTO, resIncerto.classificacao());
        assertEquals(OrigemClassificacao.PALAVRAS_CHAVE, resIncerto.origem());
    }
}
