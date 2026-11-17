package br.com.fiap.zelo.model;

import br.com.fiap.zelo.enums.EstadoCasa;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "casa_monitorada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasaMonitorada {

    @Id
    private UUID id;

    @Column(name = "idoso_id", nullable = false)
    private UUID idosoId;

    @Column(name = "nome_idoso", nullable = false)
    private String nomeIdoso;

    @Column(nullable = false)
    private String endereco;

    private String bairro;

    private String cidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_casa", nullable = false)
    private EstadoCasa estadoCasa;

    @Column(name = "consentimento_ativo", nullable = false)
    private Boolean consentimentoAtivo;

    @Column(name = "ultimo_comodo")
    private String ultimoComodo;

    @Column(name = "ultimo_estado_sensor")
    private String ultimoEstadoSensor;

    @Column(name = "ultimo_evento_em")
    private Instant ultimoEventoEm;

    @Column(name = "porta_entrada_aberta_em")
    private Instant portaEntradaAbertaEm;

    @OneToOne(mappedBy = "casa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private RegraConfiguracao regraConfiguracao;

    @OneToMany(mappedBy = "casa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Alerta> alertas = new ArrayList<>();
}
