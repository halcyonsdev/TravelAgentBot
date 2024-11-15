package com.halcyon.travelagent;

import com.halcyon.travelagent.config.Credentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
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
            log.info("TravelAgentBot successfully started.");
            Thread.currentThread().join();
        } catch (Exception e) {
            log.error("An error occurred while registering the bot.");
        }
    }
}
