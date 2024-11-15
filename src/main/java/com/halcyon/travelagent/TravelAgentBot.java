package com.halcyon.travelagent;

import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.config.Credentials;
import com.halcyon.travelagent.controller.CommandController;
import com.halcyon.travelagent.controller.TravelController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelAgentBot implements LongPollingSingleThreadUpdateConsumer {
    private final CommandController commandController;
    private final TravelController travelController;
    private final CacheManager cacheManager;

    private final TelegramClient telegramClient = new OkHttpTelegramClient(Credentials.getBotToken());

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();

            switch (message.getText()) {
                case "/start" -> commandController.handleStartCommand(this, message.getChatId());

                default -> {
                    if (message.getText().charAt(0) == '/') {
                        commandController.handleUnknownCommand(this, message);
                    } else {
                        processStatus(message);
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            switch (update.getCallbackQuery().getData()) {
                case "get_travels" -> travelController.getTravels(this, update);
                case "create_travel" -> travelController.createTravel(this, update);

                case "back" -> commandController.handleBackCommand(this, update);
            }
        }
    }

    private void processStatus(Message message) {
        Optional<ChatStatus> chatStatusOptional = cacheManager.getChatStatus(message.getChatId());

        if (chatStatusOptional.isEmpty()) {
            return;
        }

        ChatStatus chatStatus = chatStatusOptional.get();

        switch (chatStatus.getType()) {
            case TRAVEL_NAME -> travelController.changeTravelName(this, message, Long.parseLong(chatStatus.getData()));
        }
    }

    public void sendMessage(SendMessage sendMessage) {
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message.");
        }
    }

    public void editMessage(EditMessageText editMessageText) {
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message.");
        }
    }
}
