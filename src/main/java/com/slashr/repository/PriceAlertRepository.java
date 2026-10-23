package com.slashr.repository;

import com.slashr.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    @Query("select a from PriceAlert a join fetch a.game")
    List<PriceAlert> findAllComJogo();

    @Query("""
           select a from PriceAlert a join fetch a.game g
           where a.chatId = :chatId and a.telegramUserId = :userId
           order by a.createdAt
           """)
    List<PriceAlert> findDaPessoa(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("""
           select count(a) > 0 from PriceAlert a
           where a.chatId = :chatId and a.telegramUserId = :userId and a.game.itadId = :itadId
           """)
    boolean existeAlerta(@Param("chatId") Long chatId,
                         @Param("userId") Long userId,
                         @Param("itadId") String itadId);

    @Transactional
    @Modifying
    @Query("""
           delete from PriceAlert a
           where a.chatId = :chatId and a.telegramUserId = :userId and a.game.itadId = :itadId
           """)
    int removeAlerta(@Param("chatId") Long chatId,
                     @Param("userId") Long userId,
                     @Param("itadId") String itadId);
}
