package br.com.fiap.zelo.dto;

import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import jakarta.validation.constraints.Size;

public record CheckinRequestDto(
        ClassificacaoCheckin resposta,
        @Size(max = 500, message = "Texto deve ter no máximo 500 caracteres")
        String texto
) {}
