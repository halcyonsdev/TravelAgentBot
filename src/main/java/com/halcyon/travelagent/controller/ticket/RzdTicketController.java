package com.halcyon.travelagent.controller.ticket;

import com.halcyon.travelagent.api.yandex.Station;
import com.halcyon.travelagent.api.yandex.YandexAPI;
import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.generateChoiceOfStations;
import static com.halcyon.travelagent.util.KeyboardUtils.generateRzdTripsInfoKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class RzdTicketController {
    private final BotMessageHelper botMessageHelper;
    private final CacheManager cacheManager;
    private final YandexAPI yandexAPI;

    private static String DATE_PATTERN = "yyyy-MM-dd";

    public void enterTicketStartCity(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

        var enterStartCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите город, *откуда* вам нужен билет")
                .build();
        enterStartCityMessage.enableMarkdown(true);

        Message sentMessage = botMessageHelper.sendMessage(enterStartCityMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.RZD_START_CITY)
                        .data(List.of(String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void chooseTicketStartStation(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        int toDeleteMessageId = Integer.parseInt(cachedData.get(0));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        var nearestStationsOptional = yandexAPI.getNearestStations(message.getText());

        if (nearestStationsOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        List<Station> nearestStations = nearestStationsOptional.get();

        if (nearestStations.isEmpty()) {
            sendInvalidCityMessage(chatId);
            return;
        }

        var chooseStartStationMessage = SendMessage.builder()
                .chatId(chatId)
                .text("*Выберите станцию, откуда вам нужен билет:*")
                .replyMarkup(generateChoiceOfStations(nearestStations, true))
                .build();
        chooseStartStationMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(chooseStartStationMessage);
        cacheManager.remove(String.valueOf(chatId));
    }

    private void sendInvalidCityMessage(long chatId) {
        var invalidCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("К сожалению, в введенном городе не было найдено ни одной ближайшей станции. Пожалуйста, проверьте данные и попробуйте снова")
                .build();
        botMessageHelper.sendMessage(invalidCityMessage);
    }

    public void enterTicketDestinationCity(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String startStationCode = callbackQuery.getData().split("_")[3];

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

        var enterDestinationCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите город, *куда* вам нужен билет")
                .build();
        enterDestinationCityMessage.enableMarkdown(true);

        Message sentMessage = botMessageHelper.sendMessage(enterDestinationCityMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.RZD_DESTINATION_CITY)
                        .data(List.of(startStationCode, String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void chooseTicketDestinationStation(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        var nearestStationsOptional = yandexAPI.getNearestStations(message.getText());

        if (nearestStationsOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        List<Station> nearestStations = nearestStationsOptional.get();

        if (nearestStations.isEmpty()) {
            sendInvalidCityMessage(chatId);
            return;
        }

        var chooseDestinationStationMessage = SendMessage.builder()
                .chatId(chatId)
                .text("*Выберите станцию, куда вам нужен билет:*")
                .replyMarkup(generateChoiceOfStations(nearestStations, false))
                .build();
        chooseDestinationStationMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(chooseDestinationStationMessage);
    }

    public void enterStartTripDate(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String destinationStationCode = callbackQuery.getData().split("_")[3];

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

        Optional<ChatStatus> chatStatusOptional = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class);

        if (chatStatusOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        String startStationCode = chatStatusOptional.get().getData().get(0);

        var enterTripDateMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Введите дату *отправления* в формате " + DATE_PATTERN)
                .build();
        enterTripDateMessage.enableMarkdown(true);

        Message sentMessage = botMessageHelper.sendMessage(enterTripDateMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.RZD_START_DATE)
                        .data(List.of(startStationCode, destinationStationCode, String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void getRzdTickets(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        int toDeleteMessageId = Integer.parseInt(cachedData.get(2));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        String date = message.getText();
        if (!isValidDate(date)) {
            sendInvalidDateFormatMessage(chatId);
            return;
        }

        String startStationCode = cachedData.get(0);
        String destinationStationCode = cachedData.get(1);

        Optional<String> tripsInfoOptional = yandexAPI.getTrips(startStationCode, destinationStationCode, date, 0);

        if (tripsInfoOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        var tripsMessage = SendMessage.builder()
                .chatId(chatId)
                .text(tripsInfoOptional.get())
                .replyMarkup(generateRzdTripsInfoKeyboardMarkup(startStationCode, destinationStationCode, date, 0))
                .build();
        tripsMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(tripsMessage);
        cacheManager.remove(String.valueOf(chatId));
    }

    private boolean isValidDate(String text) {
        try {
            LocalDate.parse(text, DateTimeFormatter.ofPattern(DATE_PATTERN));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void sendInvalidDateFormatMessage(long chatId) {
        var invalidDateFormatMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Введенные данные не соответствуют формату " + DATE_PATTERN + ". Пожалуйста, введите дату *отправления* снова по указанному формату")
                .build();
        invalidDateFormatMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(invalidDateFormatMessage);
    }

    public void goInTripsOrder(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] callbackData = callbackQuery.getData().split("_");

        String startStationCode = callbackData[3];
        String destinationStationCode = callbackData[4];
        String date = callbackData[5];
        int order = Integer.parseInt(callbackData[6]);

        Optional<String> tripsInfoOptional = yandexAPI.getTrips(startStationCode, destinationStationCode, date, order);

        if (tripsInfoOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        var tripsMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(tripsInfoOptional.get())
                .replyMarkup(generateRzdTripsInfoKeyboardMarkup(startStationCode, destinationStationCode, date, order))
                .build();
        tripsMessage.enableMarkdown(true);

        botMessageHelper.editMessage(tripsMessage);
    }
}
