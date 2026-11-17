package br.com.fiap.zelo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegracaoExternaService {

    private final RestTemplate restTemplate;

    @Value("${app.zelo.cuidado-url:http://localhost:5000}")
    private String cuidadoUrl;

    @Value("${app.zelo.insights-url:http://localhost:8000}")
    private String insightsUrl;

    public record ContatoApoio(
            UUID contatoId,
            String nome,
            String papel,
            Integer ordem,
            String telefone,
            String canal
    ) {}

    public record RedeApoioDto(
            Boolean consentimentoAtivo,
            List<ContatoApoio> contatos
    ) {}

    public record PerfilRotinaDto(
            String horarioHabitualLevantar,
            Integer confianca
    ) {}

    public RedeApoioDto buscarRedeApoio(UUID casaId) {
        try {
            String url = cuidadoUrl + "/api/v1/casas/" + casaId + "/rede-apoio";
            RedeApoioDto resposta = restTemplate.getForObject(url, RedeApoioDto.class);
            if (resposta != null && resposta.contatos() != null && !resposta.contatos().isEmpty()) {
                return resposta;
            }
        } catch (Exception e) {
            log.debug("API .NET (.zelo-cuidado) indisponível. Utilizando mock de rede de apoio para casa {}", casaId);
        }

        return obterMockRedeApoio();
    }

    public RedeApoioDto obterMockRedeApoio() {
        return new RedeApoioDto(
                Boolean.TRUE,
                List.of(
                        new ContatoApoio(
                                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                                "Ana (Filha)",
                                "FAMILIAR",
                                1,
                                "+5511999990002",
                                "WHATSAPP"
                        ),
                        new ContatoApoio(
                                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                                "Seu José (Vizinho)",
                                "GUARDIAO",
                                2,
                                "+5511999990003",
                                "LIGACAO"
                        )
                )
        );
    }

    public PerfilRotinaDto buscarPerfilRotina(UUID casaId) {
        try {
            String url = insightsUrl + "/api/v1/casas/" + casaId + "/perfil-rotina";
            return restTemplate.getForObject(url, PerfilRotinaDto.class);
        } catch (Exception e) {
            log.debug("API Python (zelo-insights) indisponível para casa {}. Usando configuração padrão.", casaId);
            return null;
        }
    }
}
