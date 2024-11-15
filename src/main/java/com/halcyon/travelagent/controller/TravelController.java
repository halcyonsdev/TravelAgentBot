package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.TravelAgentBot;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.generateTravelsInlineKeyboard;

@Controller
@RequiredArgsConstructor
public class TravelController {
    private final TravelService travelService;

    public void getTravels(TravelAgentBot bot, Update update) {
        var message = update.getCallbackQuery().getMessage();
        List<Travel> userTravels = travelService.getUserTravels(message.getChatId());

        var newMessage = EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        newMessage.enableMarkdown(true);

        bot.editMessage(newMessage);
    }

    public void createTravel(TravelAgentBot bot, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();

        travelService.create(callbackQuery);

        var message = SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text("Пожалуйста, введите название для путешествия")
                .build();

        bot.sendMessage(message);
    }

    public void changeTravelName(TravelAgentBot bot, Message message, long travelId) {
        long chatId = message.getChatId();
        String newTravelName = message.getText();

        if (newTravelName.length() > 100) {
            sendInvalidNameMessage(bot, chatId);
        }

        travelService.changeName(travelId, newTravelName, chatId);

        sendTravelsMessage(bot, chatId, message.getFrom().getId());
    }

    private void sendInvalidNameMessage(TravelAgentBot bot, long chatId) {
        var errorMessage = SendMessage.builder()
                .chatId(chatId)
                .text("***Длина названия путешествия не дожна превышать 100 символов!*** Пожалуйта, введите название снова")
                .build();

        errorMessage.enableMarkdown(true);
        bot.sendMessage(errorMessage);
    }

    private void sendTravelsMessage(TravelAgentBot bot, long chatId, long userId) {
        List<Travel> userTravels = travelService.getUserTravels(userId);

        var message = SendMessage.builder()
                .chatId(chatId)
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        message.enableMarkdown(true);

        bot.sendMessage(message);
    }
}
