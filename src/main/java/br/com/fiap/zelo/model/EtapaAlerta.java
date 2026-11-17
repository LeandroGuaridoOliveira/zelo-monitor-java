package br.com.fiap.zelo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "alerta_etapa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaAlerta {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    @JsonIgnore
    private Alerta alerta;

    @Column(nullable = false)
    private Integer nivel;

    @Column(name = "contato_id")
    private UUID contatoId;

    @Column(name = "nome_contato")
    private String nomeContato;

    @Column(nullable = false)
    private String canal;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    @Column(name = "respondido_em")
    private Instant respondidoEm;

    private String resposta;
}
