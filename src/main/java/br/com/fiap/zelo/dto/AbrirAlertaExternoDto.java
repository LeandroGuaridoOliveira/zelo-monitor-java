package br.com.fiap.zelo.dto;

import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.OrigemAlerta;
import br.com.fiap.zelo.enums.RegraTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AbrirAlertaExternoDto(
        @NotNull(message = "O casaId é obrigatório")
        UUID casaId,

        @NotNull(message = "A regra é obrigatória")
        RegraTipo regra,

        @NotNull(message = "A gravidade é obrigatória")
        GravidadeAlerta gravidade,

        @NotNull(message = "A origem é obrigatória")
        OrigemAlerta origem,

        @NotBlank(message = "O contexto é obrigatório")
        String contexto
) {}
