package com.slashr.service;

import com.slashr.collector.GameMatch;
import com.slashr.model.Game;
import com.slashr.model.PriceAlert;
import com.slashr.repository.GameRepository;
import com.slashr.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/*
 * Grava o "quero acompanhar este jogo" vindo do clique no /add.
 * Idempotente: clique duplo ou update reprocessado não duplica nada (a restrição única do banco é a garantia final).
 * Sem @Transactional de propósito: cada chamada ao repository abre a sua transação, então a violação
 * de unicidade estoura na hora, onde dá para tratar, e não só no commit.
 */
@Service
@RequiredArgsConstructor
public class AlertaService {

    public enum Resultado {
        ADICIONADO,
        JA_EXISTIA
    }

    private final GameRepository gameRepository;
    private final PriceAlertRepository alertRepository;

    public Resultado adiciona(long chatId, long userId, String firstName, String username, GameMatch jogo) {
        Game game = achaOuCriaJogo(jogo);

        if (alertRepository.existeAlerta(chatId, userId, game.getItadId())) {
            return Resultado.JA_EXISTIA;
        }

        PriceAlert alerta = new PriceAlert();
        alerta.setChatId(chatId);
        alerta.setTelegramUserId(userId);
        alerta.setFirstName(firstName);
        alerta.setUsername(username);
        alerta.setGame(game);
        try {
            alertRepository.save(alerta);
            return Resultado.ADICIONADO;
        } catch (DataIntegrityViolationException e) {
            if (alertRepository.existeAlerta(chatId, userId, game.getItadId())) {
                return Resultado.JA_EXISTIA;
            }
            throw e;
        }
    }

    private Game achaOuCriaJogo(GameMatch jogo) {
        String itadId = jogo.id().toLowerCase();
        return gameRepository.findByItadId(itadId).orElseGet(() -> {
            Game novo = new Game();
            novo.setItadId(itadId);
            novo.setTitle(jogo.nomeJogo());
            try {
                return gameRepository.save(novo);
            } catch (DataIntegrityViolationException e) {
                return gameRepository.findByItadId(itadId).orElseThrow(() -> e);
            }
        });
    }

    public List<GameMatch> alertasDaPessoa(long chatId, long userId) {
        return alertRepository.findDaPessoa(chatId, userId).stream()
                .map(a -> new GameMatch(a.getGame().getItadId(), a.getGame().getTitle()))
                .toList();
    }

    public boolean remove(long chatId, long userId, String itadId) {
        return alertRepository.removeAlerta(chatId, userId, itadId) > 0;
    }

    public Optional<String> tituloDoJogo(String itadId) {
        return gameRepository.findByItadId(itadId).map(Game::getTitle);
    }

}
