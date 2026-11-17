package br.com.fiap.zelo.dto;

import br.com.fiap.zelo.enums.Comodo;
import br.com.fiap.zelo.enums.EstadoSensor;
import br.com.fiap.zelo.enums.TipoSensor;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventoSensorMessageDto(
        UUID eventoId,
        @NotNull(message = "casaId é obrigatório")
        UUID casaId,
        UUID sensorId,
        TipoSensor tipoSensor,
        @NotNull(message = "comodo é obrigatório")
        Comodo comodo,
        @NotNull(message = "estado é obrigatório")
        EstadoSensor estado,
        @NotNull(message = "ocorridoEm é obrigatório")
        Instant ocorridoEm,
        Boolean simulado
) {}
