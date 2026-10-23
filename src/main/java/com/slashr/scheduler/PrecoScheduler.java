package com.slashr.scheduler;

import com.slashr.service.VerificadorPrecos;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//é a "agenda", ele seta que horas o bot vai rodar
@Component
@RequiredArgsConstructor
public class PrecoScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrecoScheduler.class);

    private final VerificadorPrecos verificador;

    //segundo 0, minuto 0 de toda hora
    //o bot pode puxar o preço antes do ITAD pegar a promoção, isso atrasa 1 hora, mas tem problema não
    @Scheduled(cron = "0 0 * * * *", zone = "America/Maceio")
    public void roda() {
        try {
            verificador.verifica();
        } catch (Exception e) {
            log.error("Erro na rodada de preços ({})", e.getClass().getSimpleName());
        }
    }
}
