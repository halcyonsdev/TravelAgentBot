package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.TravelAgentBot;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.*;

@Controller
@RequiredArgsConstructor
public class TravelController {
    private final TravelService travelService;

    public void getTravels(TravelAgentBot bot, CallbackQuery callbackQuery) {
        var message = callbackQuery.getMessage();
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

    public void createTravel(TravelAgentBot bot, CallbackQuery callbackQuery) {
        travelService.create(callbackQuery);
        sendEnterDataMessage(bot, callbackQuery, "название");
    }

    private void sendEnterDataMessage(TravelAgentBot bot, CallbackQuery callbackQuery, String data) {
        var message = SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text("Пожалуйста, введите " + data + " для путешествия")
                .build();

        bot.sendMessage(message);
    }

    public void changeTravelName(TravelAgentBot bot, Message message, long travelId) {
        long chatId = message.getChatId();
        String newTravelName = message.getText();

        if (newTravelName.length() > 100) {
            sendInvalidDataMessage(
                    bot, message,
                    "***Длина названия путешествия не дожна превышать 100 символов!*** Пожалуйта, введите название снова"
            );
        }

        travelService.changeNameAndRemoveStatus(travelId, newTravelName, chatId);

        sendTravelsMessage(bot, chatId, message.getFrom().getId());
    }

    private void sendInvalidDataMessage(TravelAgentBot bot, Message message, String errorText) {
        var errorMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text(errorText)
                .replyToMessageId(message.getMessageId())
                .build();

        errorMessage.enableMarkdown(true);
        bot.sendMessage(errorMessage);
    }

    private void sendTravelsMessage(TravelAgentBot bot, long chatId, long userId) {
        List<Travel> userTravels = travelService.getUserTravels(userId);

        var travelsMessage = SendMessage.builder()
                .chatId(chatId)
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        travelsMessage.enableMarkdown(true);

        bot.sendMessage(travelsMessage);
    }

    public void getTravel(TravelAgentBot bot, CallbackQuery callbackQuery) {
        String[] splitData = callbackQuery.getData().split("_");
        long travelId = Long.parseLong(splitData[2]);

        Travel travel = travelService.findById(travelId);

        var travelInfoMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getTravelInfoText(travel, splitData))
                .replyMarkup(generateTravelInfoKeyboardMarkup(travelId, splitData.length == 5 ? Integer.parseInt(splitData[4]): -1))
                .build();
        travelInfoMessage.enableMarkdown(true);

        bot.editMessage(travelInfoMessage);
    }

    private String getTravelInfoText(Travel travel, String[] splitData) {
        return String.format("""
                        ***%s***
                        ___%s___
                        
                        ***Создано:*** %s
                        """,
                splitData.length == 5 ? "Новое путешествие " + splitData[4] : travel.getName(),
                travel.getDescription(),
                travel.getCreatedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        );
    }

    public void changeTravel(TravelAgentBot bot, CallbackQuery callbackQuery) {
        String[] splitData = callbackQuery.getData().split("_");
        long travelId = Long.parseLong(splitData[2]);

        Travel travel = travelService.findById(travelId);

        var changeTravelMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getTravelInfoText(travel, splitData))
                .replyMarkup(generateChangeTravelKeyboardMarkup(travelId))
                .build();
        changeTravelMessage.enableMarkdown(true);

        bot.editMessage(changeTravelMessage);
    }

    public void deleteTravel(TravelAgentBot bot, CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        travelService.deleteTravel(travelId);

        getTravels(bot, callbackQuery);
    }

    public void enterNewTravelName(TravelAgentBot bot, CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        travelService.changeName(travelId, "");
        travelService.saveStatus(travelId, ChatStatusType.TRAVEL_NAME, callbackQuery);

        sendEnterDataMessage(bot, callbackQuery, "название");
    }

    public void enterNewTravelDescription(TravelAgentBot bot, CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        travelService.changeDescription(travelId, "Описание отсутствует");
        travelService.saveStatus(travelId, ChatStatusType.TRAVEL_DESCRIPTION, callbackQuery);

        sendEnterDataMessage(bot, callbackQuery, "описание");
    }

    public void changeTravelDescription(TravelAgentBot bot, Message message, long travelId) {
        long chatId = message.getChatId();
        String newTravelDescription = message.getText();

        if (newTravelDescription.length() > 500) {
            sendInvalidDataMessage(
                    bot, message,
                    "***Длина описания путешествия не дожна превышать 500 символов!*** Пожалуйта, введите описание снова"
            );
        }

        travelService.changeDescriptionAndRemoveStatus(travelId, newTravelDescription, chatId);

        sendTravelsMessage(bot, chatId, message.getFrom().getId());
    }
}
