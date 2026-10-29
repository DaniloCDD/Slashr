package com.slashr.repository;

import com.slashr.model.Game;
import com.slashr.model.PriceAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:slashr-alert-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class PriceAlertRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PriceAlertRepository alertRepository;

    private Game hades;
    private Game celeste;

    @BeforeEach
    void setUp() {
        hades = saveGame("018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f", "Hades");
        celeste = saveGame("118d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f", "Celeste");
    }

    @Test
    void findAllComJogoRetornaAlertasComJogoAssociado() {
        alertRepository.saveAndFlush(alert(10L, 20L, "Ana", hades));
        alertRepository.saveAndFlush(alert(10L, 21L, "Bia", celeste));

        List<PriceAlert> resultado = alertRepository.findAllComJogo();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(a -> a.getGame().getTitle())
                .containsExactlyInAnyOrder("Hades", "Celeste");
    }

    @Test
    void findDaPessoaFiltraPorChatEUsuario() {
        alertRepository.saveAndFlush(alert(10L, 20L, "Ana", hades));
        alertRepository.saveAndFlush(alert(10L, 21L, "Bia", celeste));
        alertRepository.saveAndFlush(alert(11L, 20L, "Ana", celeste));

        List<PriceAlert> resultado = alertRepository.findDaPessoa(10L, 20L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getGame().getTitle()).isEqualTo("Hades");
    }

    @Test
    void existeAlertaReconheceApenasCombinacaoDeChatUsuarioEJogo() {
        alertRepository.saveAndFlush(alert(10L, 20L, "Ana", hades));

        assertThat(alertRepository.existeAlerta(10L, 20L, hades.getItadId())).isTrue();
        assertThat(alertRepository.existeAlerta(10L, 21L, hades.getItadId())).isFalse();
        assertThat(alertRepository.existeAlerta(11L, 20L, hades.getItadId())).isFalse();
        assertThat(alertRepository.existeAlerta(10L, 20L, celeste.getItadId())).isFalse();
    }

    @Test
    void removeAlertaRemoveSomenteAlertaDaPessoaIndicada() {
        alertRepository.saveAndFlush(alert(10L, 20L, "Ana", hades));
        alertRepository.saveAndFlush(alert(10L, 21L, "Bia", hades));

        int removidos = alertRepository.removeAlerta(10L, 20L, hades.getItadId());
        alertRepository.flush();

        assertThat(removidos).isEqualTo(1);
        assertThat(alertRepository.existeAlerta(10L, 20L, hades.getItadId())).isFalse();
        assertThat(alertRepository.existeAlerta(10L, 21L, hades.getItadId())).isTrue();
    }

    private Game saveGame(String id, String title) {
        Game game = new Game();
        game.setItadId(id);
        game.setTitle(title);
        return gameRepository.saveAndFlush(game);
    }

    private PriceAlert alert(long chatId, long userId, String firstName, Game game) {
        PriceAlert alert = new PriceAlert();
        alert.setChatId(chatId);
        alert.setTelegramUserId(userId);
        alert.setFirstName(firstName);
        alert.setGame(game);
        alert.setOnSale(false);
        return alert;
    }
}
