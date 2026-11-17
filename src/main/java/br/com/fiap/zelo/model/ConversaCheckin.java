package br.com.fiap.zelo.model;

import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import br.com.fiap.zelo.enums.OrigemClassificacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversa_checkin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversaCheckin {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    @JsonIgnore
    private Alerta alerta;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Column(name = "texto_idoso", columnDefinition = "TEXT", nullable = false)
    private String textoIdoso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassificacaoCheckin classificacao;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "resposta_para_idoso", columnDefinition = "TEXT", nullable = false)
    private String respostaParaIdoso;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_classificacao", nullable = false)
    private OrigemClassificacao origemClassificacao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
