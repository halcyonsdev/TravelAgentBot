package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.TravelAgentBot;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.halcyon.travelagent.util.KeyboardUtils.generateStartInlineKeyboard;

@Controller
public class HelpController {
    private static final String START_MESSAGE = """
                        ***🌍 Привет! Я — твой персональный туристический помощник***.
                        
                        Куда мечтаешь поехать: на пляж, в горы или исследовать мегаполисы? Я помогу построить маршрут, найти лучшие отели, билеты и все, что нужно для идеального и незабываемого путешествия.\s
                        
                        Просто скажите, что вас интересует, и я помогу вам найти лучшие предложения!
                        """;

    public void handleStartCommand(TravelAgentBot bot, long chatId) {
        SendMessage startMessage = SendMessage.builder()
                .chatId(chatId)
                .text(START_MESSAGE)
                .replyMarkup(generateStartInlineKeyboard())
                .build();
        startMessage.enableMarkdown(true);

        bot.sendMessage(startMessage);
    }

    public void handleBackCommand(TravelAgentBot bot, Update update) {
        var startMessage = EditMessageText.builder()
                .chatId(update.getCallbackQuery().getMessage().getChatId())
                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                .text(START_MESSAGE)
                .replyMarkup(generateStartInlineKeyboard())
                .build();
        startMessage.enableMarkdown(true);

        bot.editMessage(startMessage);
    }
}
