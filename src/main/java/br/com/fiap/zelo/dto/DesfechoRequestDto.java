package br.com.fiap.zelo.dto;

import br.com.fiap.zelo.enums.DesfechoAlerta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DesfechoRequestDto(
        @NotNull(message = "O desfecho é obrigatório")
        DesfechoAlerta desfecho,

        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao
) {}
