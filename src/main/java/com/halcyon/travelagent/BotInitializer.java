package com.halcyon.travelagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.halcyon.travelagent.config.Credentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotInitializer implements CommandLineRunner {
    private final TravelAgentBot travelAgentBot;

    @Override
    public void run(String... args) {
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(Credentials.getBotToken(), travelAgentBot);
            log.info("Telegram bot successfully started.");
            Thread.currentThread().join();
        } catch (Exception e) {
            log.error("An error occurred while registering the bot.");
        }
    }

    @Bean
    public ObjectMapper objectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
