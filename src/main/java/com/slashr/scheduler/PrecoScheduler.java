package com.slashr.scheduler;

import com.slashr.service.VerificadorPrecos;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara a verificação de preços a cada hora cheia(13:00,14:00,...).
 */
@Component
@RequiredArgsConstructor
public class PrecoScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrecoScheduler.class);

    private final VerificadorPrecos verificador;

    @Scheduled(cron = "0 0 * * * *", zone = "America/Maceio")
    public void roda() {
        try {
            verificador.verifica();
        } catch (Exception e) {
            //Uma rodada com erro não pode cancelar as próximas.
            log.error("Erro na rodada de preços ({})", e.getClass().getSimpleName());
        }
    }
}
