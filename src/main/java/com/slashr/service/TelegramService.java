package com.slashr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramService {

    private final RestClient restClient;

    @Value("${telegram.bot.token}")
    private String token;

    public TelegramService() {
        this.restClient = RestClient.create();
    }

    public String getMe() {

        String url = "https://api.telegram.org/bot" + token + "/getMe";

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
    public String getUpdates() {

        String url = "https://api.telegram.org/bot" + token + "/getUpdates";

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}