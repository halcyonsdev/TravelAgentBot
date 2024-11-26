package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
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
    private final CacheManager cacheManager;
    private final BotMessageHelper botMessageHelper;

    public void getTravels(CallbackQuery callbackQuery) {
        var message = callbackQuery.getMessage();
        List<Travel> userTravels = travelService.getUserTravels(message.getChatId());

        var newMessage = EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        newMessage.enableMarkdown(true);

        botMessageHelper.editMessage(newMessage);
    }

    public void createTravel(CallbackQuery callbackQuery) {
        long travelId = travelService.createTravel(callbackQuery).getId();

        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(ChatStatusType.TRAVEL_NAME)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );

        sendEnterDataMessage(callbackQuery, "название");
    }

    private void sendEnterDataMessage(CallbackQuery callbackQuery, String data) {
        var message = SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text("Пожалуйста, введите " + data + " для путешествия")
                .build();

        botMessageHelper.sendMessage(message);
    }

    public void changeTravelName(Message message, long travelId) {
        long chatId = message.getChatId();
        String newTravelName = message.getText();

        if (newTravelName.length() > 100) {
            sendInvalidDataMessage(
                    message,
                    "*Длина названия путешествия не дожна превышать 100 символов!* Пожалуйта, введите название снова"
            );
        } else {
            travelService.changeName(travelId, newTravelName);
            cacheManager.remove(String.valueOf(chatId));

            sendTravelsMessage(chatId, message.getFrom().getId());
        }
    }

    private void sendInvalidDataMessage(Message message, String errorText) {
        var errorMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text(errorText)
                .replyToMessageId(message.getMessageId())
                .build();

        errorMessage.enableMarkdown(true);
        botMessageHelper.sendMessage(errorMessage);
    }

    private void sendTravelsMessage(long chatId, long userId) {
        List<Travel> userTravels = travelService.getUserTravels(userId);

        var travelsMessage = SendMessage.builder()
                .chatId(chatId)
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        travelsMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(travelsMessage);
    }

    public void getTravel(CallbackQuery callbackQuery) {
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

        botMessageHelper.editMessage(travelInfoMessage);
    }

    private String getTravelInfoText(Travel travel, String[] splitData) {
        return String.format("""
                        *🌍️ Название:* %s
                        *📖 Описание:* ___%s___
                        *🕒 Создано:* %s
                        """,
                splitData.length == 5 ? "Новое путешествие " + splitData[4] : travel.getName(),
                travel.getDescription(),
                travel.getCreatedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );
    }

    public void changeTravel(CallbackQuery callbackQuery) {
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

        botMessageHelper.editMessage(changeTravelMessage);
    }

    public void deleteTravel(CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        travelService.deleteTravel(travelId);

        getTravels(callbackQuery);
    }

    public void enterNewTravelName(CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        travelService.changeName(travelId, "");

        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(ChatStatusType.TRAVEL_NAME)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );

        sendEnterDataMessage(callbackQuery, "название");
    }

    public void enterNewTravelDescription(CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        travelService.changeDescription(travelId, "отсутствует");

        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(ChatStatusType.TRAVEL_DESCRIPTION)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );

        sendEnterDataMessage(callbackQuery, "описание");
    }

    public void changeTravelDescription(Message message, long travelId) {
        long chatId = message.getChatId();
        String newTravelDescription = message.getText();

        if (newTravelDescription.length() > 500) {
            sendInvalidDataMessage(
                    message,
                    "*Длина описания путешествия не должна превышать 500 символов!* Пожалуйта, введите описание снова"
            );
        }

        travelService.changeDescription(travelId, newTravelDescription);
        cacheManager.remove(String.valueOf(chatId));

        sendTravelsMessage(chatId, message.getFrom().getId());
    }
}