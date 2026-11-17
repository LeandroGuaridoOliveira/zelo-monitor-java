package br.com.fiap.zelo.dto;

import java.util.UUID;

public record ConfirmacaoRequestDto(
        UUID contatoId,
        String nomeContato
) {}
