package br.com.fiap.zelo.service;

import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import br.com.fiap.zelo.enums.OrigemClassificacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CheckinAiService {

    private final Optional<OpenAiChatClient> chatClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    private static final List<String> PALAVRAS_RISCO = List.of(
            "caí", "cai", "queda", "tonta", "tonto", "ajuda", "socorro",
            "falta de ar", "não consigo", "nao consigo", "mal", "fraca", "fraco",
            "sangue", "peito", "esquisita", "esquisito"
    );

    private static final Pattern PATTERN_DOR = Pattern.compile(".*\\bdores?\\b.*", Pattern.CASE_INSENSITIVE);

    private static final List<String> PALAVRAS_BEM_ESTAR = List.of(
            "estou bem", "tô bem", "to bem", "tudo bem", "tudo certo",
            "só dormi", "so dormi", "já tomei", "ja tomei", "estou ótima", "estou otima", "estou ótimo", "estou otimo"
    );

    public record ResultadoCheckin(
            ClassificacaoCheckin classificacao,
            String respostaParaIdoso,
            OrigemClassificacao origem,
            String motivo
    ) {}

    public CheckinAiService(
            Optional<OpenAiChatClient> chatClient,
            @Value("${spring.ai.openai.api-key:chave-nao-configurada}") String apiKey,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    public ResultadoCheckin classificarTextoIdoso(String texto, String contextoAlerta) {
        if (texto == null || texto.isBlank()) {
            return new ResultadoCheckin(
                    ClassificacaoCheckin.INCERTO,
                    "Não consegui entender bem, Dona Maria. Por precaução, vou pedir para a Ana falar com você.",
                    OrigemClassificacao.PALAVRAS_CHAVE,
                    "Texto vazio ou ausente"
            );
        }

        boolean keyValida = apiKey != null
                && !apiKey.isBlank()
                && !apiKey.equalsIgnoreCase("chave-nao-configurada")
                && !apiKey.equalsIgnoreCase("sua-chave-aqui");

        if (chatClient.isPresent() && keyValida) {
            try {
                String promptText = """
                        Você é o assistente virtual do sistema de cuidado aos idosos Zelo.
                        O idoso recebeu um alerta de monitoramento com contexto: "%s".
                        O idoso respondeu ao check-in dizendo: "%s".

                        Classifique a resposta estritamente em uma das 3 categorias:
                        - ESTOU_BEM: se o idoso expressa claramente que está bem, seguro e sem problemas.
                        - PRECISO_AJUDA: se o idoso relata dor, queda, tontura, mal-estar ou pede ajuda.
                        - INCERTO: se a resposta for ambígua, confusa ou desconexa.

                        Gere também uma resposta acolhedora de até 2 frases em português para o idoso, sem conselho médico e sem desencorajar pedido de ajuda.

                        Responda OBRIGATORIAMENTE em JSON puro no formato:
                        {"classificacao": "ESTOU_BEM|PRECISO_AJUDA|INCERTO", "resposta": "...", "motivo": "..."}
                        """.formatted(contextoAlerta != null ? contextoAlerta : "", texto);

                ChatResponse response = chatClient.get().call(new Prompt(promptText));
                String content = response.getResult().getOutput().getContent();

                if (content != null && !content.isBlank()) {
                    String cleanJson = content.trim();
                    if (cleanJson.startsWith("```")) {
                        cleanJson = cleanJson.replaceFirst("^```json\\s*", "")
                                             .replaceFirst("^```\\s*", "")
                                             .replaceAll("```$", "")
                                             .trim();
                    }

                    JsonNode jsonNode = objectMapper.readTree(cleanJson);
                    String classStr = jsonNode.path("classificacao").asText("INCERTO").toUpperCase().trim();
                    String resposta = jsonNode.path("resposta").asText();
                    String motivo = jsonNode.path("motivo").asText("Classificado por LLM");

                    ClassificacaoCheckin classificacao;
                    try {
                        classificacao = ClassificacaoCheckin.valueOf(classStr);
                    } catch (Exception e) {
                        classificacao = ClassificacaoCheckin.INCERTO;
                    }

                    // Guardrail de segurança obrigatório: se houver palavra de risco, nunca permite ESTOU_BEM
                    if (contemPalavraRisco(texto) && classificacao == ClassificacaoCheckin.ESTOU_BEM) {
                        log.warn("Guardrail acionado: LLM classificou como ESTOU_BEM, mas texto '{}' contém palavra de risco. Forçando PRECISO_AJUDA.", texto);
                        classificacao = ClassificacaoCheckin.PRECISO_AJUDA;
                        resposta = "Entendi, Dona Maria. Fique tranquila, já estou avisando a Ana para ajudar você.";
                        motivo = "Guardrail de segurança acionado devido a palavras de risco no texto";
                    }

                    if (resposta.isBlank()) {
                        resposta = (classificacao == ClassificacaoCheckin.ESTOU_BEM)
                                ? "Que bom saber que você está bem, Dona Maria! Tenha um ótimo dia."
                                : "Entendi, Dona Maria. Fique tranquila, já estou avisando a Ana para ajudar você.";
                    }

                    return new ResultadoCheckin(classificacao, resposta, OrigemClassificacao.LLM, motivo);
                }
            } catch (Exception e) {
                log.warn("Falha ao consultar LLM OpenAiChatClient, acionando fallback por palavras-chave: {}", e.getMessage());
            }
        }

        return fallbackPalavrasChave(texto);
    }

    public ResultadoCheckin fallbackPalavrasChave(String texto) {
        if (contemPalavraRisco(texto)) {
            return new ResultadoCheckin(
                    ClassificacaoCheckin.PRECISO_AJUDA,
                    "Entendi, Dona Maria. Fique tranquila, já estou avisando a Ana para ajudar você.",
                    OrigemClassificacao.PALAVRAS_CHAVE,
                    "Palavra de risco identificada no texto"
            );
        }

        if (contemPalavraBemEstar(texto)) {
            return new ResultadoCheckin(
                    ClassificacaoCheckin.ESTOU_BEM,
                    "Que bom saber que você está bem, Dona Maria! Tenha um ótimo dia.",
                    OrigemClassificacao.PALAVRAS_CHAVE,
                    "Afirmação de bem-estar identificada no texto"
            );
        }

        return new ResultadoCheckin(
                ClassificacaoCheckin.INCERTO,
                "Não consegui entender bem, Dona Maria. Por precaução, vou pedir para a Ana falar com você.",
                OrigemClassificacao.PALAVRAS_CHAVE,
                "Texto ambíguo ou não conclusivo"
        );
    }

    public boolean contemPalavraRisco(String texto) {
        if (texto == null) return false;
        String lower = texto.toLowerCase();
        if (PATTERN_DOR.matcher(lower).matches()) {
            return true;
        }
        return PALAVRAS_RISCO.stream().anyMatch(lower::contains);
    }

    public boolean contemPalavraBemEstar(String texto) {
        if (texto == null) return false;
        String lower = texto.toLowerCase();
        return PALAVRAS_BEM_ESTAR.stream().anyMatch(lower::contains);
    }
}
