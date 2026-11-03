package com.slashr.repository;

import com.slashr.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    // join fetch traz o jogo na mesma consulta;
    // sem ele, o acesso a getGame() fora do repository daria erro(LazyInitializationException).
    @Query("select a from PriceAlert a join fetch a.game")
    List<PriceAlert> findAllComJogo();

    @Query("""
           select a from PriceAlert a join fetch a.game g
           where a.chatId = :chatId and a.telegramUserId = :userId
           order by a.createdAt
           """)
    List<PriceAlert> findDaPessoa(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Verifica se a pessoa já acompanha o jogo.
    @Query("""
           select count(a) > 0 from PriceAlert a
           where a.chatId = :chatId and a.telegramUserId = :userId and a.game.itadId = :itadId
           """)
    boolean existeAlerta(@Param("chatId") Long chatId,
                         @Param("userId") Long userId,
                         @Param("itadId") String itadId);


    // Devolve quantos alertas foram removidos (0 = a pessoa já não acompanhava o jogo).
    @Transactional
    @Modifying
    @Query("""
       delete from PriceAlert a
       where a.chatId = :chatId and a.telegramUserId = :userId
         and a.game.id in (select g.id from Game g where g.itadId = :itadId)
       """)
    int removeAlerta(@Param("chatId") Long chatId,
                     @Param("userId") Long userId,
                     @Param("itadId") String itadId);
}
