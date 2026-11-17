package br.com.fiap.zelo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegraConfiguracaoDto(
        @NotBlank(message = "Horário habitual de levantar é obrigatório")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário deve estar no formato HH:mm")
        String horarioHabitualLevantar,

        @NotNull(message = "Tolerância matinal é obrigatória")
        @Min(value = 10, message = "Tolerância matinal mínima é 10 minutos")
        @Max(value = 360, message = "Tolerância matinal máxima é 360 minutos")
        Integer toleranciaMatinalMin,

        @NotNull(message = "Limite de banheiro é obrigatório")
        @Min(value = 5, message = "Limite de banheiro mínimo é 5 minutos")
        @Max(value = 180, message = "Limite de banheiro máximo é 180 minutos")
        Integer limiteBanheiroMin,

        @NotNull(message = "Limite de inatividade é obrigatório")
        @Min(value = 30, message = "Limite de inatividade mínimo é 30 minutos")
        @Max(value = 720, message = "Limite de inatividade máximo é 720 minutos")
        Integer limiteInatividadeMin,

        @NotNull(message = "Espera checkin crítico é obrigatória")
        @Min(value = 1, message = "Espera checkin crítico mínimo é 1 minuto")
        @Max(value = 60, message = "Espera checkin crítico máximo é 60 minutos")
        Integer esperaCheckinCriticoMin,

        @NotNull(message = "Espera escalada crítico é obrigatória")
        @Min(value = 1, message = "Espera escalada crítico mínimo é 1 minuto")
        @Max(value = 60, message = "Espera escalada crítico máximo é 60 minutos")
        Integer esperaEscaladaCriticoMin,

        @NotNull(message = "Espera checkin atenção é obrigatória")
        @Min(value = 1, message = "Espera checkin atenção mínimo é 1 minuto")
        @Max(value = 120, message = "Espera checkin atenção máximo é 120 minutos")
        Integer esperaCheckinAtencaoMin,

        @NotNull(message = "Espera escalada atenção é obrigatória")
        @Min(value = 1, message = "Espera escalada atenção mínimo é 1 minuto")
        @Max(value = 120, message = "Espera escalada atenção máximo é 120 minutos")
        Integer esperaEscaladaAtencaoMin
) {}
