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

import java.time.Instant;
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
    private final Map<UUID, Map<String, Object>> registrosHoje = new ConcurrentHashMap<>();
    private final Map<UUID, List<Map<String, Object>>> contatosPorIdoso = new ConcurrentHashMap<>();

    public static final UUID IDOSO_DONA_MARIA = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID CASA_DONA_MARIA = UUID.fromString("00000000-0000-0000-0000-000000000100");
    public static final UUID LEMBRETE_1_ID = UUID.fromString("00000000-0000-0000-0000-000000002001");
    public static final UUID REGISTRO_1_ID = UUID.fromString("00000000-0000-0000-0000-000000003001");

    public CuidadoMirrorController() {
        // Lembrete inicial 1: Remédio de pressão (Losartana 50mg)
        Map<String, Object> l1 = new HashMap<>();
        l1.put("id", LEMBRETE_1_ID);
        l1.put("idosoId", IDOSO_DONA_MARIA);
        l1.put("titulo", "Remédio da pressão (Losartana 50mg)");
        l1.put("descricao", "Remédio da pressão (Losartana 50mg)");
        l1.put("texto", "Remédio da pressão (Losartana 50mg)");
        l1.put("horario", "08:00");
        l1.put("horarioPrevisto", "08:00");
        l1.put("dosagem", "1 comprimido pela manhã");
        l1.put("diasSemana", "SEG,TER,QUA,QUI,SEX,SAB,DOM");
        l1.put("ativo", true);
        l1.put("criadoEm", Instant.now().toString());
        lembretes.put(LEMBRETE_1_ID, l1);

        // Registro de hoje inicial para o lembrete 1
        Map<String, Object> r1 = new HashMap<>();
        r1.put("id", REGISTRO_1_ID);
        r1.put("registroId", REGISTRO_1_ID);
        r1.put("lembreteId", LEMBRETE_1_ID);
        r1.put("idosoId", IDOSO_DONA_MARIA);
        r1.put("titulo", "Remédio da pressão (Losartana 50mg)");
        r1.put("descricao", "Remédio da pressão (Losartana 50mg)");
        r1.put("horario", "08:00");
        r1.put("horarioPrevisto", "08:00");
        r1.put("status", "PENDENTE");
        r1.put("tomado", false);
        r1.put("tomadoEm", null);
        r1.put("dataPrevista", LocalDate.now().toString());
        registrosHoje.put(REGISTRO_1_ID, r1);

        // Contatos da Dona Maria (Ana e Seu José)
        List<Map<String, Object>> contatos = new ArrayList<>();
        UUID contato1Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Map<String, Object> c1 = new HashMap<>();
        c1.put("id", contato1Id);
        c1.put("contatoId", contato1Id);
        c1.put("idosoId", IDOSO_DONA_MARIA);
        c1.put("nome", "Ana (Filha)");
        c1.put("papel", "FAMILIAR");
        c1.put("parentesco", "Filha");
        c1.put("telefone", "+5511999990002");
        c1.put("ordem", 1);
        c1.put("ordemPrioridade", 1);
        c1.put("canal", "WHATSAPP");
        c1.put("notificarPor", "WHATSAPP");
        contatos.add(c1);

        UUID contato2Id = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Map<String, Object> c2 = new HashMap<>();
        c2.put("id", contato2Id);
        c2.put("contatoId", contato2Id);
        c2.put("idosoId", IDOSO_DONA_MARIA);
        c2.put("nome", "Seu José (Vizinho)");
        c2.put("papel", "GUARDIAO");
        c2.put("parentesco", "Vizinho");
        c2.put("telefone", "+5511999990003");
        c2.put("ordem", 2);
        c2.put("ordemPrioridade", 2);
        c2.put("canal", "LIGACAO");
        c2.put("notificarPor", "LIGACAO");
        contatos.add(c2);

        contatosPorIdoso.put(IDOSO_DONA_MARIA, contatos);
    }

    @GetMapping("/health")
    @Operation(summary = "Healthcheck do serviço zelo-monitor")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "servico", "zelo-monitor"));
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

        String desc = (String) body.getOrDefault("descricao", body.getOrDefault("titulo", body.getOrDefault("texto", "Lembrete")));
        novoLembrete.put("descricao", desc);
        novoLembrete.put("titulo", desc);
        novoLembrete.put("texto", desc);

        String hora = (String) body.getOrDefault("horario", body.getOrDefault("horarioPrevisto", "08:00"));
        novoLembrete.put("horario", hora);
        novoLembrete.put("horarioPrevisto", hora);

        if (!novoLembrete.containsKey("diasSemana")) {
            novoLembrete.put("diasSemana", "SEG,TER,QUA,QUI,SEX,SAB,DOM");
        }
        if (!novoLembrete.containsKey("ativo")) {
            novoLembrete.put("ativo", true);
        }
        novoLembrete.put("criadoEm", Instant.now().toString());
        lembretes.put(novoId, novoLembrete);

        // Cria registro de hoje correspondente
        UUID regId = UUID.randomUUID();
        Map<String, Object> reg = new HashMap<>();
        reg.put("id", regId);
        reg.put("registroId", regId);
        reg.put("lembreteId", novoId);
        reg.put("idosoId", id);
        reg.put("titulo", desc);
        reg.put("descricao", desc);
        reg.put("horario", hora);
        reg.put("horarioPrevisto", hora);
        reg.put("status", "PENDENTE");
        reg.put("tomado", false);
        reg.put("tomadoEm", null);
        reg.put("dataPrevista", LocalDate.now().toString());
        registrosHoje.put(regId, reg);

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

        String desc = (String) existente.getOrDefault("descricao", existente.getOrDefault("titulo", existente.getOrDefault("texto", "Lembrete")));
        existente.put("descricao", desc);
        existente.put("titulo", desc);
        existente.put("texto", desc);

        String hora = (String) existente.getOrDefault("horario", existente.getOrDefault("horarioPrevisto", "08:00"));
        existente.put("horario", hora);
        existente.put("horarioPrevisto", hora);

        lembretes.put(id, existente);

        // Atualiza registros de hoje vinculados
        for (Map<String, Object> reg : registrosHoje.values()) {
            if (id.equals(reg.get("lembreteId"))) {
                reg.put("descricao", desc);
                reg.put("titulo", desc);
                reg.put("horario", hora);
                reg.put("horarioPrevisto", hora);
            }
        }

        return ResponseEntity.ok(existente);
    }

    @DeleteMapping("/lembretes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um lembrete")
    public ResponseEntity<Void> removerLembrete(@PathVariable UUID id) {
        lembretes.remove(id);
        registrosHoje.values().removeIf(reg -> id.equals(reg.get("lembreteId")));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/idosos/{id}/lembretes/hoje")
    @Operation(summary = "Lista os lembretes com status do dia para o idoso")
    public ResponseEntity<List<Map<String, Object>>> listarLembretesHoje(@PathVariable UUID id) {
        List<Map<String, Object>> result = registrosHoje.values().stream()
                .filter(r -> id.equals(r.get("idosoId")))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/lembretes/registros/{id}/confirmacao")
    @Operation(summary = "Confirma a tomada do medicamento pelo idoso")
    public ResponseEntity<Map<String, Object>> confirmarTomada(@PathVariable UUID id) {
        Map<String, Object> registro = registrosHoje.get(id);
        if (registro == null) {
            // Tenta encontrar por lembreteId ou id de fallback
            registro = registrosHoje.values().stream()
                    .filter(r -> id.equals(r.get("lembreteId")) || id.equals(r.get("id")) || id.equals(r.get("registroId")))
                    .findFirst()
                    .orElse(null);
        }

        if (registro == null) {
            registro = new HashMap<>();
            registro.put("id", id);
            registro.put("registroId", id);
            registrosHoje.put(id, registro);
        }

        registro.put("status", "CONFIRMADO");
        registro.put("tomado", true);
        registro.put("tomadoEm", LocalTime.now().toString());

        return ResponseEntity.ok(registro);
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
        UUID cid = body.containsKey("contatoId") ? (UUID) body.get("contatoId") : UUID.randomUUID();

        Map<String, Object> novoContato = new HashMap<>(body);
        novoContato.put("id", cid);
        novoContato.put("contatoId", cid);
        novoContato.put("idosoId", id);

        int ordem = lista.size() + 1;
        novoContato.put("ordem", ordem);
        novoContato.put("ordemPrioridade", ordem);

        String canal = (String) body.getOrDefault("canal", body.getOrDefault("notificarPor", "WHATSAPP"));
        novoContato.put("canal", canal);
        novoContato.put("notificarPor", canal);

        String papel = (String) body.getOrDefault("papel", body.getOrDefault("parentesco", "FAMILIAR"));
        novoContato.put("papel", papel);
        novoContato.put("parentesco", papel);

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
    public ResponseEntity<List<Map<String, Object>>> obterResumoSemanal(@PathVariable UUID id) {
        Map<String, Object> metricas = Map.of(
                "totalEventosPresenca", 342,
                "horarioLevantarMedio", "07:22",
                "diasAtipicos", 1,
                "lembretesCumpridosPct", 95
        );

        Map<String, Object> resumo = Map.of(
                "casaId", id,
                "semanaInicio", LocalDate.now().minusDays(7).toString(),
                "texto", "Dona Maria apresentou rotina consistente ao longo da semana, com média de despertar às 07:22 e uso regular do banheiro (média de 18 min). Apenas 1 dia atípico registrado.",
                "metricas", metricas,
                "modelo", "GPT-4o-mini (Zelo Insights)",
                "geradoEm", Instant.now().toString()
        );

        return ResponseEntity.ok(List.of(resumo));
    }

    @GetMapping("/casas/{id}/dias-atipicos")
    @Operation(summary = "Lista os dias com desvios atípicos de rotina identificados")
    public ResponseEntity<List<Map<String, Object>>> listarDiasAtipicos(@PathVariable UUID id) {
        Map<String, Object> atributos = Map.of(
                "levantarMinutos", 450,
                "eventosMadrugada", 3,
                "tempoSemMovimentoMax", 180
        );

        Map<String, Object> diaAtipico = Map.of(
                "data", LocalDate.now().minusDays(2).toString(),
                "score", 0.82,
                "atipico", true,
                "atributos", atributos
        );

        return ResponseEntity.ok(List.of(diaAtipico));
    }
}
