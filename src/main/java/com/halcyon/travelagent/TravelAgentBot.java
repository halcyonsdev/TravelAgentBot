package com.halcyon.travelagent;

import com.halcyon.travelagent.config.Credentials;
import com.halcyon.travelagent.controller.HelpController;
import com.halcyon.travelagent.controller.TravelController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelAgentBot implements LongPollingSingleThreadUpdateConsumer {
    private final HelpController helpController;
    private final TravelController travelController;

    private final TelegramClient telegramClient = new OkHttpTelegramClient(Credentials.getBotToken());

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();

            switch (message.getText()) {
                case "/start" -> helpController.handleStartCommand(this, message.getChatId());
            }
        } else if (update.hasCallbackQuery()) {
            switch (update.getCallbackQuery().getData()) {
                case "get_travels" -> travelController.handleGettingTravels(this, update);
                case "back" -> helpController.handleBackCommand(this, update);
            }
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
