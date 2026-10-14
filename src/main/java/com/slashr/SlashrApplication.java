package com.slashr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlashrApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlashrApplication.class, args);
    }
}