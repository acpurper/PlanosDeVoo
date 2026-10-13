package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "trecho")
public class TrechoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_id")
    private PlanoVooEntity plano;

    private int ordem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aerovia_id")
    private AeroviaEntity aerovia;

    @Column(name = "altitude_pes")
    private int altitudePes;

    @Column(name = "horario_entrada")
    private LocalDateTime horarioEntrada;

    /** Nulo quando o trecho termina em sobrevoo. */
    @ManyToOne
    @JoinColumn(name = "pouso_vaga_id")
    private VagaEntity pousoVaga;

    @Column(name = "pouso_permanencia_min")
    private Integer pousoPermanenciaMin;

    protected TrechoEntity() {
    }

    public TrechoEntity(PlanoVooEntity plano, int ordem, AeroviaEntity aerovia, int altitudePes,
                        LocalDateTime horarioEntrada, VagaEntity pousoVaga, Integer pousoPermanenciaMin) {
        this.plano = plano;
        this.ordem = ordem;
        this.aerovia = aerovia;
        this.altitudePes = altitudePes;
        this.horarioEntrada = horarioEntrada;
        this.pousoVaga = pousoVaga;
        this.pousoPermanenciaMin = pousoPermanenciaMin;
    }

    public int getOrdem() { return ordem; }
    public AeroviaEntity getAerovia() { return aerovia; }
    public int getAltitudePes() { return altitudePes; }
    public LocalDateTime getHorarioEntrada() { return horarioEntrada; }
    public VagaEntity getPousoVaga() { return pousoVaga; }
    public Integer getPousoPermanenciaMin() { return pousoPermanenciaMin; }
}
