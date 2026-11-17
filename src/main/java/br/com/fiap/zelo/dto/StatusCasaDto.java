package br.com.fiap.zelo.dto;

import br.com.fiap.zelo.enums.EstadoCasa;
import br.com.fiap.zelo.model.Alerta;

import java.util.UUID;

public record StatusCasaDto(
        UUID casaId,
        String nomeIdoso,
        EstadoCasa estadoCasa,
        UltimoEventoResumoDto ultimoEvento,
        Alerta alertaAberto,
        String horarioAtualSimulado
) {}
