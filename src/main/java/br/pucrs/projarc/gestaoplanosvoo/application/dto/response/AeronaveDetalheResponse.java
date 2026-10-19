package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveCarga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveParticular;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronavePassageiros;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.RegraOperacao;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Aeronave com os dados próprios da categoria. Uma categoria nova sem ramo aqui ainda é listada,
 * só sem os campos específicos dela.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AeronaveDetalheResponse(String prefixo, String categoria, double velocidadeCruzeiroKmh,
                                      double autonomiaKm, double autonomiaSeguraKm, String empresaManutencao,
                                      String companhiaAerea, Integer capacidadePassageiros,
                                      Double capacidadeToneladas, List<String> regrasOperacao) {

    public static AeronaveDetalheResponse de(Aeronave a) {
        String manutencao = null;
        String companhia = null;
        Integer passageiros = null;
        Double toneladas = null;
        switch (a) {
            case AeronaveParticular p -> manutencao = p.getEmpresaManutencao();
            case AeronavePassageiros p -> {
                companhia = p.getCompanhiaAerea();
                passageiros = p.getCapacidadePassageiros();
            }
            case AeronaveCarga c -> {
                companhia = c.getCompanhiaAerea();
                toneladas = c.getCapacidadeToneladas();
            }
            default -> { }
        }
        return new AeronaveDetalheResponse(a.getPrefixo(), a.categoria(), a.getVelocidadeCruzeiroKmh(),
                a.getAutonomiaKm(), a.autonomiaSegura(), manutencao, companhia, passageiros, toneladas,
                a.regras().stream().map(RegraOperacao::descricao).toList());
    }
}
