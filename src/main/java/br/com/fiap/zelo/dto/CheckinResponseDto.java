package br.com.fiap.zelo.dto;

import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import br.com.fiap.zelo.enums.EstadoAlerta;
import br.com.fiap.zelo.enums.OrigemClassificacao;

import java.util.UUID;

public record CheckinResponseDto(
        UUID alertaId,
        EstadoAlerta estadoAlerta,
        ClassificacaoCheckin classificacao,
        String respostaParaIdoso,
        OrigemClassificacao origemClassificacao,
        String motivo
) {}
