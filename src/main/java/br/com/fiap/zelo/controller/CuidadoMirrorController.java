package br.com.fiap.zelo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@Tag(name = "Cuidado e Insights (Espelho Autônomo)", description = "Endpoints espelho de apoio ao mobile para Lembretes, Contatos e Resumos da Rotina")
public class CuidadoMirrorController {

    private final Map<UUID, Map<String, Object>> lembretes = new ConcurrentHashMap<>();
    private final Map<UUID, List<Map<String, Object>>> contatosPorIdoso = new ConcurrentHashMap<>();

    private static final UUID IDOSO_DONA_MARIA = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID CASA_DONA_MARIA = UUID.fromString("00000000-0000-0000-0000-000000000100");

    public CuidadoMirrorController() {
        // Lembretes iniciais da Dona Maria
        UUID lembrete1Id = UUID.fromString("00000000-0000-0000-0000-000000000501");
        Map<String, Object> l1 = new HashMap<>();
        l1.put("id", lembrete1Id);
        l1.put("idosoId", IDOSO_DONA_MARIA);
        l1.put("titulo", "Losartana 50mg");
        l1.put("horarioPrevisto", "08:00");
        l1.put("dosagem", "1 comprimido pela manhã");
        l1.put("ativo", true);
        lembretes.put(lembrete1Id, l1);

        UUID lembrete2Id = UUID.fromString("00000000-0000-0000-0000-000000000502");
        Map<String, Object> l2 = new HashMap<>();
        l2.put("id", lembrete2Id);
        l2.put("idosoId", IDOSO_DONA_MARIA);
        l2.put("titulo", "Metformina 850mg");
        l2.put("horarioPrevisto", "12:30");
        l2.put("dosagem", "1 comprimido no almoço");
        l2.put("ativo", true);
        lembretes.put(lembrete2Id, l2);

        // Contatos da Dona Maria
        List<Map<String, Object>> contatos = new ArrayList<>();
        contatos.add(Map.of(
                "contatoId", UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "nome", "Ana (Filha)",
                "papel", "FAMILIAR",
                "ordem", 1,
                "telefone", "+5511999990002",
                "canal", "WHATSAPP"
        ));
        contatos.add(Map.of(
                "contatoId", UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "nome", "Seu José (Vizinho)",
                "papel", "GUARDIAO",
                "ordem", 2,
                "telefone", "+5511999990003",
                "canal", "LIGACAO"
        ));
        contatosPorIdoso.put(IDOSO_DONA_MARIA, contatos);
    }

    @GetMapping("/idosos/{id}/lembretes")
    @Operation(summary = "Lista todos os lembretes configurados para o idoso")
    public ResponseEntity<List<Map<String, Object>>> listarLembretes(@PathVariable UUID id) {
        List<Map<String, Object>> result = lembretes.values().stream()
                .filter(l -> id.equals(l.get("idosoId")))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/idosos/{id}/lembretes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo lembrete para o idoso")
    public ResponseEntity<Map<String, Object>> criarLembrete(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID novoId = UUID.randomUUID();
        Map<String, Object> novoLembrete = new HashMap<>(body);
        novoLembrete.put("id", novoId);
        novoLembrete.put("idosoId", id);
        if (!novoLembrete.containsKey("ativo")) novoLembrete.put("ativo", true);
        lembretes.put(novoId, novoLembrete);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoLembrete);
    }

    @PutMapping("/lembretes/{id}")
    @Operation(summary = "Atualiza um lembrete existente")
    public ResponseEntity<Map<String, Object>> atualizarLembrete(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> existente = lembretes.get(id);
        if (existente == null) {
            existente = new HashMap<>();
            existente.put("id", id);
        }
        existente.putAll(body);
        lembretes.put(id, existente);
        return ResponseEntity.ok(existente);
    }

    @DeleteMapping("/lembretes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um lembrete")
    public ResponseEntity<Void> removerLembrete(@PathVariable UUID id) {
        lembretes.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/idosos/{id}/lembretes/hoje")
    @Operation(summary = "Lista os lembretes com status do dia para o idoso")
    public ResponseEntity<List<Map<String, Object>>> listarLembretesHoje(@PathVariable UUID id) {
        List<Map<String, Object>> result = lembretes.values().stream()
                .filter(l -> id.equals(l.get("idosoId")))
                .map(l -> {
                    Map<String, Object> item = new HashMap<>(l);
                    item.put("registroId", UUID.randomUUID());
                    item.put("dataPrevista", LocalDate.now().toString());
                    item.put("status", "PENDENTE");
                    return item;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/lembretes/registros/{id}/confirmacao")
    @Operation(summary = "Confirma a tomada do medicamento pelo idoso")
    public ResponseEntity<Map<String, Object>> confirmarTomada(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of(
                "registroId", id,
                "status", "CONFIRMADO",
                "confirmadoEm", LocalTime.now().toString()
        ));
    }

    @GetMapping("/idosos/{id}/contatos")
    @Operation(summary = "Lista contatos cadastrados para o idoso")
    public ResponseEntity<List<Map<String, Object>>> listarContatos(@PathVariable UUID id) {
        List<Map<String, Object>> contatos = contatosPorIdoso.getOrDefault(id, List.of());
        return ResponseEntity.ok(contatos);
    }

    @PostMapping("/idosos/{id}/contatos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona um novo contato de apoio")
    public ResponseEntity<Map<String, Object>> criarContato(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> lista = contatosPorIdoso.computeIfAbsent(id, k -> new ArrayList<>());
        Map<String, Object> novoContato = new HashMap<>(body);
        if (!novoContato.containsKey("contatoId")) novoContato.put("contatoId", UUID.randomUUID());
        novoContato.put("ordem", lista.size() + 1);
        lista.add(novoContato);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoContato);
    }

    @GetMapping("/casas/{id}/rede-apoio")
    @Operation(summary = "Retorna a rede de apoio configurada para a casa")
    public ResponseEntity<Map<String, Object>> obterRedeApoio(@PathVariable UUID id) {
        List<Map<String, Object>> contatos = contatosPorIdoso.getOrDefault(IDOSO_DONA_MARIA, List.of());
        return ResponseEntity.ok(Map.of(
                "consentimentoAtivo", true,
                "contatos", contatos
        ));
    }

    @GetMapping("/casas/{id}/resumos")
    @Operation(summary = "Retorna o resumo analítico semanal da rotina gerado pelo Zelo Insights")
    public ResponseEntity<Map<String, Object>> obterResumoSemanal(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of(
                "casaId", id,
                "periodo", "Últimos 7 dias",
                "indiceRegularidadeRotina", 92,
                "tempoMedioBanheiroMin", 18,
                "horarioMedioLevantar", "07:22",
                "desviosDetectados", 1,
                "statusGeral", "ESTAVEL",
                "observacao", "Dona Maria mantém padrão diário estável, com pequenas variações nos finais de semana."
        ));
    }

    @GetMapping("/casas/{id}/dias-atipicos")
    @Operation(summary = "Lista os dias com desvios atípicos de rotina identificados")
    public ResponseEntity<List<Map<String, Object>>> listarDiasAtipicos(@PathVariable UUID id) {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "data", LocalDate.now().minusDays(2).toString(),
                        "tipo", "BANHEIRO_LONGO",
                        "descricao", "Dona Maria permaneceu 35 min no banheiro (habitual: 18 min)",
                        "gravidade", "MEDIA"
                )
        ));
    }
}
