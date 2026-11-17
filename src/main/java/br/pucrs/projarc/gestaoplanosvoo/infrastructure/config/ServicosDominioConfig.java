package br.pucrs.projarc.gestaoplanosvoo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.GestaoReservas;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;

@Configuration
public class ServicosDominioConfig {

    /** Os casos de uso recebem {@link GestaoReservas} por injeção; aqui se escolhe a implementação. */
    @Bean
    GestaoReservas gestaoReservas(ReservaRepositoryPort reservas) {
        return new ReservaService(reservas);
    }
}

