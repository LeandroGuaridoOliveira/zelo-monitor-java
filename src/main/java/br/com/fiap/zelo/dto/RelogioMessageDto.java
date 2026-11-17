package br.com.fiap.zelo.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record RelogioMessageDto(
        @NotNull(message = "casaId é obrigatório")
        UUID casaId,

        @NotNull(message = "agora é obrigatório")
        Instant agora
) {}
