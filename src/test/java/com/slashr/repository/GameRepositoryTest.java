package com.slashr.repository;

import com.slashr.model.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:slashr-game-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class GameRepositoryTest {

    @Autowired
    private GameRepository repository;

    @Test
    void findByItadIdEncontraJogoSalvo() {
        Game game = new Game();
        game.setItadId("018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f");
        game.setTitle("Hades");
        repository.saveAndFlush(game);

        Optional<Game> resultado = repository.findByItadId(game.getItadId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitle()).isEqualTo("Hades");
    }

    @Test
    void findByItadIdRetornaVazioParaJogoInexistente() {
        assertThat(repository.findByItadId("nao-existe")).isEmpty();
    }
}
