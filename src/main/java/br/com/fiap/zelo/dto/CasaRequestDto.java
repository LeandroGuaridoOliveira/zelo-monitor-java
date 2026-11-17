package br.com.fiap.zelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CasaRequestDto(
        @NotNull(message = "O idosoId é obrigatório")
        UUID idosoId,

        @NotBlank(message = "O nome do idoso é obrigatório")
        String nomeIdoso,

        @NotBlank(message = "O endereço é obrigatório")
        String endereco,

        String bairro,
        String cidade,
        Boolean consentimentoAtivo
) {}
