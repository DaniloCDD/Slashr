package com.slashr.service;

import com.slashr.collector.GameMatch;
import com.slashr.model.Game;
import com.slashr.model.PriceAlert;
import com.slashr.repository.GameRepository;
import com.slashr.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AlertaServiceTest {

    private static final String ID = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";

    private GameRepository gameRepository;
    private PriceAlertRepository alertRepository;
    private AlertaService service;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        alertRepository = mock(PriceAlertRepository.class);
        service = new AlertaService(gameRepository, alertRepository);
    }

    @Test
    void adicionaCriaJogoEAlertaQuandoAindaNaoExistem() {
        when(gameRepository.findByItadId(ID)).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.existeAlerta(10L, 20L, ID)).thenReturn(false);
        when(alertRepository.save(any(PriceAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertaService.Resultado resultado = service.adiciona(10L, 20L, "Ana", "ana", new GameMatch(ID.toUpperCase(), "Hades"));

        assertThat(resultado).isEqualTo(AlertaService.Resultado.ADICIONADO);
        verify(gameRepository).save(argThat(game -> ID.equals(game.getItadId()) && "Hades".equals(game.getTitle())));
        verify(alertRepository).save(argThat(alert ->
                alert.getChatId() == 10L && alert.getTelegramUserId() == 20L
                        && "Ana".equals(alert.getFirstName()) && "ana".equals(alert.getUsername())
                        && "Hades".equals(alert.getGame().getTitle())));
    }

    @Test
    void adicionaReutilizaJogoExistente() {
        Game existente = game(ID, "Hades");
        when(gameRepository.findByItadId(ID)).thenReturn(Optional.of(existente));
        when(alertRepository.existeAlerta(10L, 20L, ID)).thenReturn(false);
        when(alertRepository.save(any(PriceAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.adiciona(10L, 20L, "Ana", null, new GameMatch(ID, "Outro título")))
                .isEqualTo(AlertaService.Resultado.ADICIONADO);

        verify(gameRepository, never()).save(any(Game.class));
        verify(alertRepository).save(argThat(alert -> alert.getGame() == existente && alert.getUsername() == null));
    }

    @Test
    void adicionaRetornaJaExistiaSemSalvarAlertaDuplicado() {
        Game existente = game(ID, "Hades");
        when(gameRepository.findByItadId(ID)).thenReturn(Optional.of(existente));
        when(alertRepository.existeAlerta(10L, 20L, ID)).thenReturn(true);

        assertThat(service.adiciona(10L, 20L, "Ana", null, new GameMatch(ID, "Hades")))
                .isEqualTo(AlertaService.Resultado.JA_EXISTIA);

        verify(alertRepository, never()).save(any(PriceAlert.class));
    }

    @Test
    void adicionaTrataCorridaDeInsercaoDuplicadaQuandoAlertaJaExiste() {
        Game existente = game(ID, "Hades");
        when(gameRepository.findByItadId(ID)).thenReturn(Optional.of(existente));
        when(alertRepository.existeAlerta(10L, 20L, ID)).thenReturn(false, true);
        when(alertRepository.save(any(PriceAlert.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThat(service.adiciona(10L, 20L, "Ana", null, new GameMatch(ID, "Hades")))
                .isEqualTo(AlertaService.Resultado.JA_EXISTIA);
    }

    @Test
    void adicionaPropagaViolacaoDeIntegridadeQuandoNaoEraDuplicidadeDeAlerta() {
        Game existente = game(ID, "Hades");
        when(gameRepository.findByItadId(ID)).thenReturn(Optional.of(existente));
        when(alertRepository.existeAlerta(10L, 20L, ID)).thenReturn(false, false);
        when(alertRepository.save(any(PriceAlert.class))).thenThrow(new DataIntegrityViolationException("outra restrição"));

        assertThatThrownBy(() -> service.adiciona(10L, 20L, "Ana", null, new GameMatch(ID, "Hades")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void alertasDaPessoaConverteAlertasEmGameMatch() {
        PriceAlert alerta = new PriceAlert();
        alerta.setGame(game(ID, "Hades"));
        PriceAlert segundo = new PriceAlert();
        segundo.setGame(game("game-2", "Celeste"));
        when(alertRepository.findDaPessoa(10L, 20L)).thenReturn(List.of(alerta, segundo));

        assertThat(service.alertasDaPessoa(10L, 20L))
                .containsExactly(new GameMatch(ID, "Hades"), new GameMatch("game-2", "Celeste"));
    }

    @Test
    void removeRetornaTrueSomenteQuandoRepositorioRemoveAlgo() {
        when(alertRepository.removeAlerta(10L, 20L, ID)).thenReturn(1, 0);

        assertThat(service.remove(10L, 20L, ID)).isTrue();
        assertThat(service.remove(10L, 20L, ID)).isFalse();
        verify(alertRepository, times(2)).removeAlerta(10L, 20L, ID);
    }

    @Test
    void tituloDoJogoRetornaTituloOuOptionalVazio() {
        when(gameRepository.findByItadId(ID)).thenReturn(Optional.of(game(ID, "Hades")));
        when(gameRepository.findByItadId("ausente")).thenReturn(Optional.empty());

        assertThat(service.tituloDoJogo(ID)).contains("Hades");
        assertThat(service.tituloDoJogo("ausente")).isEmpty();
    }

    private static Game game(String itadId, String title) {
        Game game = new Game();
        game.setItadId(itadId);
        game.setTitle(title);
        return game;
    }
}
