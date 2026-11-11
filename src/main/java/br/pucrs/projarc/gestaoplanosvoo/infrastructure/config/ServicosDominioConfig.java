package br.pucrs.projarc.gestaoplanosvoo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;

@Configuration
public class ServicosDominioConfig {

    @Bean
    ReservaService reservaService(ReservaRepositoryPort reservas) {
        return new ReservaService(reservas);
    }
}

