package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plano_voo")
public class PlanoVooEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "piloto_id")
    private PilotoEntity piloto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aeronave_prefixo")
    private AeronaveEntity aeronave;

    @ManyToOne(optional = false)
    @JoinColumn(name = "origem")
    private AeroportoEntity origem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destino")
    private AeroportoEntity destino;

    @Column(name = "data_hora_partida")
    private LocalDateTime dataHoraPartida;

    @Enumerated(EnumType.STRING)
    private SituacaoPlano situacao;

    /** Lock otimista: duas edições simultâneas do mesmo plano não se sobrescrevem. */
    @Version
    private Long versao;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<TrechoEntity> trechos = new ArrayList<>();

    public PlanoVooEntity() {
    }

    public Long getId() { return id; }
    public PilotoEntity getPiloto() { return piloto; }
    public void setPiloto(PilotoEntity piloto) { this.piloto = piloto; }
    public AeronaveEntity getAeronave() { return aeronave; }
    public void setAeronave(AeronaveEntity aeronave) { this.aeronave = aeronave; }
    public AeroportoEntity getOrigem() { return origem; }
    public void setOrigem(AeroportoEntity origem) { this.origem = origem; }
    public AeroportoEntity getDestino() { return destino; }
    public void setDestino(AeroportoEntity destino) { this.destino = destino; }
    public LocalDateTime getDataHoraPartida() { return dataHoraPartida; }
    public void setDataHoraPartida(LocalDateTime dataHoraPartida) { this.dataHoraPartida = dataHoraPartida; }
    public SituacaoPlano getSituacao() { return situacao; }
    public void setSituacao(SituacaoPlano situacao) { this.situacao = situacao; }
    public Long getVersao() { return versao; }
    public List<TrechoEntity> getTrechos() { return trechos; }
}
