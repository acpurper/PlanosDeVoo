package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AltitudeTest {

    @ParameterizedTest
    @ValueSource(ints = {25_000, 30_000, 35_000})
    void aceitaNiveisValidos(int pes) {
        assertThat(new Altitude(pes).pes()).isEqualTo(pes);
    }

    @ParameterizedTest
    @ValueSource(ints = {24_000, 36_000, 25_500, 0})
    void rejeitaForaDaFaixaOuDoPasso(int pes) {
        assertThatThrownBy(() -> new Altitude(pes))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining(String.valueOf(pes));
    }

    @Test
    void existemOnzeNiveis() {
        assertThat(Altitude.todas()).hasSize(11);
    }
}
