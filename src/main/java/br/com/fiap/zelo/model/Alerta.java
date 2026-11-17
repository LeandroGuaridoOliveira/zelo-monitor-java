package br.com.fiap.zelo.model;

import br.com.fiap.zelo.enums.DesfechoAlerta;
import br.com.fiap.zelo.enums.EstadoAlerta;
import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.OrigemAlerta;
import br.com.fiap.zelo.enums.RegraTipo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casa_id", nullable = false)
    private CasaMonitorada casa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegraTipo regra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GravidadeAlerta gravidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemAlerta origem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAlerta estado;

    @Column(columnDefinition = "TEXT")
    private String contexto;

    @Column(name = "nivel_escalada_atual", nullable = false)
    @Builder.Default
    private Integer nivelEscaladaAtual = 0;

    @Column(name = "aberto_em", nullable = false)
    private Instant abertoEm;

    @Column(name = "proxima_acao_em")
    private Instant proximaAcaoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @Enumerated(EnumType.STRING)
    private DesfechoAlerta desfecho;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @OneToMany(mappedBy = "alerta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("enviadoEm ASC")
    @Builder.Default
    private List<EtapaAlerta> etapas = new ArrayList<>();

    @OneToMany(mappedBy = "alerta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("criadoEm ASC")
    @Builder.Default
    private List<ConversaCheckin> conversas = new ArrayList<>();
}
