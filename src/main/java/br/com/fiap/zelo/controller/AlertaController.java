package br.com.fiap.zelo.controller;

import br.com.fiap.zelo.dto.AbrirAlertaExternoDto;
import br.com.fiap.zelo.dto.CheckinRequestDto;
import br.com.fiap.zelo.dto.CheckinResponseDto;
import br.com.fiap.zelo.dto.ConfirmacaoRequestDto;
import br.com.fiap.zelo.dto.DesfechoRequestDto;
import br.com.fiap.zelo.dto.StatusCasaDto;
import br.com.fiap.zelo.dto.UltimoEventoResumoDto;
import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import br.com.fiap.zelo.enums.EstadoAlerta;
import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.OrigemAlerta;
import br.com.fiap.zelo.enums.OrigemClassificacao;
import br.com.fiap.zelo.enums.RegraTipo;
import br.com.fiap.zelo.model.Alerta;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.model.ConversaCheckin;
import br.com.fiap.zelo.repository.AlertaRepository;
import br.com.fiap.zelo.repository.CasaMonitoradaRepository;
import br.com.fiap.zelo.repository.ConversaCheckinRepository;
import br.com.fiap.zelo.service.CheckinAiService;
import br.com.fiap.zelo.service.MaquinaEstadosAlertaService;
import br.com.fiap.zelo.service.RelogioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alertas e Monitoramento", description = "Endpoints de alertas, check-in, escalada, confirmação e emergência")
public class AlertaController {

    private final AlertaRepository alertaRepository;
    private final CasaMonitoradaRepository casaRepository;
    private final ConversaCheckinRepository conversaRepository;
    private final MaquinaEstadosAlertaService maquinaEstadosService;
    private final CheckinAiService checkinAiService;
    private final RelogioService relogioService;

    @GetMapping("/casas/{id}/status")
    @Operation(summary = "Retorna o status atual de uma casa monitorada, último evento e alerta aberto se houver")
    public ResponseEntity<StatusCasaDto> obterStatusCasa(@PathVariable UUID id) {
        CasaMonitorada casa = casaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Casa não encontrada: " + id));

        Instant agora = relogioService.obterAgora(id);
        Alerta alertaAberto = maquinaEstadosService.buscarAlertaAberto(id).orElse(null);

        UltimoEventoResumoDto ultimoEvento = new UltimoEventoResumoDto(
                casa.getUltimoComodo(),
                casa.getUltimoEstadoSensor(),
                casa.getUltimoEventoEm()
        );

        String horarioSimuladoFormatado = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withZone(ZoneId.of("America/Sao_Paulo"))
                .format(agora);

        StatusCasaDto status = new StatusCasaDto(
                casa.getId(),
                casa.getNomeIdoso(),
                casa.getEstadoCasa(),
                ultimoEvento,
                alertaAberto,
                horarioSimuladoFormatado
        );

        return ResponseEntity.ok(status);
    }

    @GetMapping("/alertas")
    @Operation(summary = "Lista alertas paginados com filtros opcionais por casaId e estado")
    public ResponseEntity<Page<Alerta>> listarAlertas(
            @RequestParam(required = false) UUID casaId,
            @RequestParam(required = false) EstadoAlerta estado,
            @PageableDefault(page = 0, size = 10, sort = "abertoEm") Pageable pageable) {

        if (casaId != null && estado != null) {
            return ResponseEntity.ok(alertaRepository.findByCasaIdAndEstado(casaId, estado, pageable));
        } else if (casaId != null) {
            return ResponseEntity.ok(alertaRepository.findByCasaId(casaId, pageable));
        } else if (estado != null) {
            return ResponseEntity.ok(alertaRepository.findByEstado(estado, pageable));
        } else {
            return ResponseEntity.ok(alertaRepository.findAll(pageable));
        }
    }

    @GetMapping("/alertas/{id}")
    @Operation(summary = "Consulta detalhes de um alerta por ID")
    public ResponseEntity<Alerta> buscarAlertaPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(maquinaEstadosService.buscarAlerta(id));
    }

    @PostMapping("/alertas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre um alerta externo (ex.: lembrete não confirmado R5)")
    public ResponseEntity<Alerta> abrirAlertaExterno(@Valid @RequestBody AbrirAlertaExternoDto dto) {
        Instant agora = relogioService.obterAgora(dto.casaId());
        Alerta alerta = maquinaEstadosService.abrirAlerta(
                dto.casaId(),
                dto.regra(),
                dto.gravidade(),
                dto.origem(),
                dto.contexto(),
                agora
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(alerta);
    }

    @PostMapping("/casas/{id}/emergencia")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Aciona botão de emergência E1 (pula check-in e escala imediatamente)")
    public ResponseEntity<Alerta> acionarEmergencia(@PathVariable UUID id) {
        Instant agora = relogioService.obterAgora(id);
        Alerta alerta = maquinaEstadosService.abrirAlerta(
                id,
                RegraTipo.E1,
                GravidadeAlerta.CRITICO,
                OrigemAlerta.EMERGENCIA,
                "Botão de emergência acionado",
                agora
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(alerta);
    }

    @PostMapping("/alertas/{id}/checkin")
    @Operation(summary = "Processa check-in do idoso via clique em botão ou texto livre analisado por Spring AI")
    public ResponseEntity<CheckinResponseDto> responderCheckin(
            @PathVariable UUID id,
            @RequestBody CheckinRequestDto dto) {

        Alerta alerta = maquinaEstadosService.buscarAlerta(id);
        Instant agora = relogioService.obterAgora(alerta.getCasa().getId());

        ClassificacaoCheckin classificacao;
        String respostaParaIdoso;
        OrigemClassificacao origem;
        String motivo;
        String textoRegistrado;

        if (dto.resposta() != null) {
            // Check-in por botão rápido no aplicativo
            classificacao = dto.resposta();
            origem = OrigemClassificacao.BOTAO;
            motivo = "Resposta selecionada diretamente via botão de check-in";
            textoRegistrado = (dto.texto() != null && !dto.texto().isBlank()) ? dto.texto() : dto.resposta().name();
            respostaParaIdoso = (classificacao == ClassificacaoCheckin.ESTOU_BEM)
                    ? "Que bom saber que você está bem, Dona Maria! Tenha um ótimo dia."
                    : "Entendi, Dona Maria. Fique tranquila, já estou avisando a Ana para ajudar você.";
        } else {
            // Check-in por texto livre (processado por Spring AI com fallback)
            textoRegistrado = dto.texto() != null ? dto.texto() : "";
            CheckinAiService.ResultadoCheckin resultado = checkinAiService.classificarTextoIdoso(textoRegistrado, alerta.getContexto());
            classificacao = resultado.classificacao();
            respostaParaIdoso = resultado.respostaParaIdoso();
            origem = resultado.origem();
            motivo = resultado.motivo();
        }

        // Grava no histórico de conversas do check-in
        ConversaCheckin conversa = ConversaCheckin.builder()
                .id(UUID.randomUUID())
                .alerta(alerta)
                .casaId(alerta.getCasa().getId())
                .textoIdoso(textoRegistrado)
                .classificacao(classificacao)
                .motivo(motivo)
                .respostaParaIdoso(respostaParaIdoso)
                .origemClassificacao(origem)
                .criadoEm(agora)
                .build();
        conversaRepository.save(conversa);

        // Transiciona a máquina de estados
        Alerta alertaAtualizado = maquinaEstadosService.processarRespostaCheckin(id, classificacao, agora);

        CheckinResponseDto response = new CheckinResponseDto(
                alertaAtualizado.getId(),
                alertaAtualizado.getEstado(),
                classificacao,
                respostaParaIdoso,
                origem,
                motivo
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/alertas/{id}/confirmacao")
    @Operation(summary = "Cuidador confirma recebimento do alerta ('Vou verificar'), transicionando para EM_VERIFICACAO")
    public ResponseEntity<Alerta> confirmarAlerta(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmacaoRequestDto dto) {

        Alerta alerta = maquinaEstadosService.buscarAlerta(id);
        Instant agora = relogioService.obterAgora(alerta.getCasa().getId());

        UUID contatoId = dto != null ? dto.contatoId() : null;
        String nomeContato = dto != null && dto.nomeContato() != null ? dto.nomeContato() : "Cuidador";

        Alerta atualizado = maquinaEstadosService.confirmarAlerta(id, contatoId, nomeContato, agora);
        return ResponseEntity.ok(atualizado);
    }

    @PostMapping("/alertas/{id}/desfecho")
    @Operation(summary = "Cuidador encerra o alerta registrando o desfecho e observação final")
    public ResponseEntity<Alerta> registrarDesfecho(
            @PathVariable UUID id,
            @Valid @RequestBody DesfechoRequestDto dto) {

        Alerta alerta = maquinaEstadosService.buscarAlerta(id);
        Instant agora = relogioService.obterAgora(alerta.getCasa().getId());

        Alerta encerrado = maquinaEstadosService.registrarDesfecho(id, dto.desfecho(), dto.observacao(), agora);
        return ResponseEntity.ok(encerrado);
    }
}
