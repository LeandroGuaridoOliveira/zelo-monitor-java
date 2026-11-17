package br.com.fiap.zelo.dto;

import java.time.Instant;

public record UltimoEventoResumoDto(
        String comodo,
        String estado,
        Instant ocorridoEm
) {}
