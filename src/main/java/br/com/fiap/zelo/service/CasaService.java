package br.com.fiap.zelo.service;

import br.com.fiap.zelo.dto.CasaRequestDto;
import br.com.fiap.zelo.dto.RegraConfiguracaoDto;
import br.com.fiap.zelo.enums.EstadoCasa;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.model.RegraConfiguracao;
import br.com.fiap.zelo.repository.CasaMonitoradaRepository;
import br.com.fiap.zelo.repository.RegraConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasaService {

    private final CasaMonitoradaRepository casaRepository;
    private final RegraConfiguracaoRepository regraRepository;

    @Transactional(readOnly = true)
    public Page<CasaMonitorada> listarCasas(Pageable pageable) {
        return casaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CasaMonitorada buscarPorId(UUID id) {
        return casaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Casa não encontrada: " + id));
    }

    @Transactional
    public CasaMonitorada criarCasa(CasaRequestDto dto) {
        UUID casaId = UUID.randomUUID();
        CasaMonitorada casa = CasaMonitorada.builder()
                .id(casaId)
                .idosoId(dto.idosoId())
                .nomeIdoso(dto.nomeIdoso())
                .endereco(dto.endereco())
                .bairro(dto.bairro())
                .cidade(dto.cidade())
                .estadoCasa(EstadoCasa.EM_CASA)
                .consentimentoAtivo(dto.consentimentoAtivo() != null ? dto.consentimentoAtivo() : Boolean.TRUE)
                .build();

        RegraConfiguracao regra = RegraConfiguracao.builder()
                .id(UUID.randomUUID())
                .casa(casa)
                .horarioHabitualLevantar("07:20")
                .toleranciaMatinalMin(90)
                .limiteBanheiroMin(40)
                .limiteInatividadeMin(240)
                .esperaCheckinCriticoMin(3)
                .esperaEscaladaCriticoMin(5)
                .esperaCheckinAtencaoMin(15)
                .esperaEscaladaAtencaoMin(15)
                .build();

        casa.setRegraConfiguracao(regra);
        return casaRepository.save(casa);
    }

    @Transactional
    public CasaMonitorada atualizarCasa(UUID id, CasaRequestDto dto) {
        CasaMonitorada casa = buscarPorId(id);
        casa.setIdosoId(dto.idosoId());
        casa.setNomeIdoso(dto.nomeIdoso());
        casa.setEndereco(dto.endereco());
        casa.setBairro(dto.bairro());
        casa.setCidade(dto.cidade());
        if (dto.consentimentoAtivo() != null) {
            casa.setConsentimentoAtivo(dto.consentimentoAtivo());
        }
        return casaRepository.save(casa);
    }

    @Transactional
    public void removerCasa(UUID id) {
        CasaMonitorada casa = buscarPorId(id);
        casaRepository.delete(casa);
    }

    @Transactional(readOnly = true)
    public RegraConfiguracao buscarRegras(UUID casaId) {
        // Garante que a casa existe
        buscarPorId(casaId);
        return regraRepository.findByCasaId(casaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Regra de configuração não encontrada para a casa: " + casaId));
    }

    @Transactional
    public RegraConfiguracao atualizarRegras(UUID casaId, RegraConfiguracaoDto dto) {
        RegraConfiguracao regra = buscarRegras(casaId);
        regra.setHorarioHabitualLevantar(dto.horarioHabitualLevantar());
        regra.setToleranciaMatinalMin(dto.toleranciaMatinalMin());
        regra.setLimiteBanheiroMin(dto.limiteBanheiroMin());
        regra.setLimiteInatividadeMin(dto.limiteInatividadeMin());
        regra.setEsperaCheckinCriticoMin(dto.esperaCheckinCriticoMin());
        regra.setEsperaEscaladaCriticoMin(dto.esperaEscaladaCriticoMin());
        regra.setEsperaCheckinAtencaoMin(dto.esperaCheckinAtencaoMin());
        regra.setEsperaEscaladaAtencaoMin(dto.esperaEscaladaAtencaoMin());
        return regraRepository.save(regra);
    }
}
