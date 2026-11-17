package br.com.fiap.zelo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "regra_configuracao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegraConfiguracao {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casa_id", nullable = false, unique = true)
    @JsonIgnore
    private CasaMonitorada casa;

    @Column(name = "horario_habitual_levantar", nullable = false)
    @Builder.Default
    private String horarioHabitualLevantar = "07:20";

    @Column(name = "tolerancia_matinal_min", nullable = false)
    @Builder.Default
    private Integer toleranciaMatinalMin = 90;

    @Column(name = "limite_banheiro_min", nullable = false)
    @Builder.Default
    private Integer limiteBanheiroMin = 40;

    @Column(name = "limite_inatividade_min", nullable = false)
    @Builder.Default
    private Integer limiteInatividadeMin = 240;

    @Column(name = "espera_checkin_critico_min", nullable = false)
    @Builder.Default
    private Integer esperaCheckinCriticoMin = 3;

    @Column(name = "espera_escalada_critico_min", nullable = false)
    @Builder.Default
    private Integer esperaEscaladaCriticoMin = 5;

    @Column(name = "espera_checkin_atencao_min", nullable = false)
    @Builder.Default
    private Integer esperaCheckinAtencaoMin = 15;

    @Column(name = "espera_escalada_atencao_min", nullable = false)
    @Builder.Default
    private Integer esperaEscaladaAtencaoMin = 15;
}
