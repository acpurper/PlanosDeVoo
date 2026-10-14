package br.pucrs.projarc.gestaoplanosvoo.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Relógio injetável. Por padrão é o relógio do sistema; com app.relogio-fixo (ex.: 2026-11-20T10:00)
 * o "agora" fica fixo, o que torna testes e apresentações reproduzíveis.
 */
@Configuration
public class RelogioConfig {

    @Bean
    Clock clock(@Value("${app.relogio-fixo:}") String relogioFixo) {
        ZoneId zona = ZoneId.systemDefault();
        if (relogioFixo == null || relogioFixo.isBlank()) {
            return Clock.system(zona);
        }
        return Clock.fixed(LocalDateTime.parse(relogioFixo).atZone(zona).toInstant(), zona);
    }
}
