package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.bot.BotMessageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static com.halcyon.travelagent.util.KeyboardUtils.generateStartInlineKeyboard;

@Controller
@RequiredArgsConstructor
public class CommandController {
    private final BotMessageHelper botMessageHelper;

    private static final String START_MESSAGE = """
                        *🌍 Привет! Я — твой персональный туристический помощник*.
                        
                        Куда мечтаешь поехать: на пляж, в горы или исследовать мегаполисы? Я помогу построить маршрут, найти лучшие отели, билеты и все, что нужно для идеального и незабываемого путешествия.\s
                        
                        Просто скажите, что вас интересует, и я помогу вам найти лучшие предложения!
                        """;

    public void handleStartCommand(long chatId) {
        var startMessage = SendMessage.builder()
                .chatId(chatId)
                .text(START_MESSAGE)
                .replyMarkup(generateStartInlineKeyboard())
                .build();
        startMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(startMessage);
    }

    public void handleBackCommand(CallbackQuery callbackQuery) {
        var startMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(START_MESSAGE)
                .replyMarkup(generateStartInlineKeyboard())
                .build();
        startMessage.enableMarkdown(true);

        botMessageHelper.editMessage(startMessage);
    }

    public void handleBackWithDeleteCommand(CallbackQuery callbackQuery) {
        int messageId = Integer.parseInt(callbackQuery.getData().split("_")[1]);
        botMessageHelper.deleteMessage(callbackQuery.getMessage().getChatId(), messageId);

        handleBackCommand(callbackQuery);
    }

    public void handleUnknownCommand(Message message) {
        var unknownCommandMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text("***Неизвестная команда***")
                .replyToMessageId(message.getMessageId())
                .build();
        unknownCommandMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(unknownCommandMessage);
    }
}
