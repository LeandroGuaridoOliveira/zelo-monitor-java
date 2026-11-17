package br.com.fiap.zelo.dto;

import java.util.UUID;

public record AuthResponseDto(
        String accessToken,
        String token,
        String tokenType,
        Long expiresIn,
        UUID usuarioId,
        String nome,
        String email,
        String papel,
        UUID idosoId,
        UUID casaId
) {}
