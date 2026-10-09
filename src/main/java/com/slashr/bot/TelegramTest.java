package com.slashr.bot;

import com.slashr.service.TelegramService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TelegramTest implements CommandLineRunner {

    private final TelegramService telegramService;

    public TelegramTest(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public void run(String... args) {
        System.out.println(telegramService.getUpdates());
    }
}