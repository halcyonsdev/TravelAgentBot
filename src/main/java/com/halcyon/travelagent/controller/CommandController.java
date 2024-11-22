package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.TravelAgentBot;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static com.halcyon.travelagent.util.KeyboardUtils.generateStartInlineKeyboard;

@Controller
public class CommandController {
    private static final String START_MESSAGE = """
                        *🌍 Привет! Я — твой персональный туристический помощник*.
                        
                        Куда мечтаешь поехать: на пляж, в горы или исследовать мегаполисы? Я помогу построить маршрут, найти лучшие отели, билеты и все, что нужно для идеального и незабываемого путешествия.\s
                        
                        Просто скажите, что вас интересует, и я помогу вам найти лучшие предложения!
                        """;

    public void handleStartCommand(TravelAgentBot bot, long chatId) {
        var startMessage = SendMessage.builder()
                .chatId(chatId)
                .text(START_MESSAGE)
                .replyMarkup(generateStartInlineKeyboard())
                .build();
        startMessage.enableMarkdown(true);

        bot.sendMessage(startMessage);
    }

    public void handleBackCommand(TravelAgentBot bot, CallbackQuery callbackQuery) {
        var startMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(START_MESSAGE)
                .replyMarkup(generateStartInlineKeyboard())
                .build();
        startMessage.enableMarkdown(true);

        bot.editMessage(startMessage);
    }

    public void handleUnknownCommand(TravelAgentBot bot, Message message) {
        var unknownCommandMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text("***Неизвестная команда.***")
                .replyToMessageId(message.getMessageId())
                .build();
        unknownCommandMessage.enableMarkdown(true);

        bot.sendMessage(unknownCommandMessage);
    }
}
