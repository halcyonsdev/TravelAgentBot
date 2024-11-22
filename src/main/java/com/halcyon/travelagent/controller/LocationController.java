package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.TravelAgentBot;
import com.halcyon.travelagent.api.geoapify.GeoapifyAPI;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.*;

@Controller
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;
    private final CacheManager cacheManager;
    private final GeoapifyAPI geoapifyAPI;

    private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm";

    public void enterLocationCity(TravelAgentBot bot, CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        sendEnterCityMessage(bot, chatId);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.LOCATION_CITY)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );
    }

    private void sendEnterCityMessage(TravelAgentBot bot, long chatId) {
        var enterCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите название города, который хотите посетить")
                .build();

        bot.sendMessage(enterCityMessage);
    }

    public void createLocation(TravelAgentBot bot, Message message, long travelId) {
        long chatId = message.getChatId();
        Optional<List<String>> locationsOptional = geoapifyAPI.getLocations(message.getText());

        if (locationsOptional.isPresent()) {
            List<String> locations = locationsOptional.get();

            if (locations.size() == 1) {
                enterTime(bot, chatId, List.of(String.valueOf(travelId), locations.get(0)), true);
            } else {
                getChoiceOfLocations(bot, chatId, travelId, locations);
            }
        } else {
            sendEnterCityMessage(bot, chatId);
        }
    }

    private void enterTime(TravelAgentBot bot, long chatId, List<String> data, boolean isStart) {
        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(isStart ? ChatStatusType.LOCATION_START_TIME : ChatStatusType.LOCATION_END_TIME)
                        .data(data)
                        .build()
        );

        sendEnterTimeMessage(bot, chatId, isStart);
    }

    private void sendEnterTimeMessage(TravelAgentBot bot, long chatId, boolean isStart) {
        String type = isStart ? "отправления" : "прибытия";
        String text = String.format("Пожалуйста, введите время %s (в формате %s)", type, DATE_PATTERN);

        var enterTimeMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        bot.sendMessage(enterTimeMessage);
    }

    public void setLocationStartTime(TravelAgentBot bot, Message message, long travelId, String locationName) {
        long chatId = message.getChatId();

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(message.getText(), DateTimeFormatter.ofPattern(DATE_PATTERN));
            localDateTime.atZone(ZoneId.systemDefault()).toInstant();

            List<String> data = List.of(String.valueOf(travelId), locationName, message.getText());
            enterTime(bot, chatId, data, false);
        } catch (DateTimeParseException e) {
            sendErrorTimeMessage(bot, chatId, true);
        }
    }

    private void sendErrorTimeMessage(TravelAgentBot bot, long chatId, boolean isStart) {
        String type = isStart ? "отправления" : "прибытия";
        String text = String.format(
                "Введенные данные не соответствуют формату %s. Пожалуйста, введите время %s по указанному формату.",
                DATE_PATTERN, type
        );

        var errorTimeMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        bot.sendMessage(errorTimeMessage);
    }

    public void setLocationEndTime(TravelAgentBot bot, Message message, long travelId, String locationName, String locationStartTime) {
        long chatId = message.getChatId();

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

            LocalDateTime endDateTime = LocalDateTime.parse(message.getText(), dateTimeFormatter);
            Instant endTime = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

            LocalDateTime startDateTime = LocalDateTime.parse(locationStartTime, dateTimeFormatter);
            Instant startTime = startDateTime.atZone(ZoneId.systemDefault()).toInstant();

            Location location = locationService.createLocation(locationName, startTime, endTime, travelId);
            cacheManager.remove(String.valueOf(chatId));

            sendLocationInfoMessage(bot, chatId, location);
        } catch (DateTimeParseException e) {
            sendErrorTimeMessage(bot, chatId, false);
        }
    }

    private void sendLocationInfoMessage(TravelAgentBot bot, long chatId, Location location) {
        var locationInfoMessage = SendMessage.builder()
                .chatId(chatId)
                .text(getLocationInfoText(location))
                .replyMarkup(generateLocationInfoKeyboardMarkup())
                .build();
        locationInfoMessage.enableMarkdown(true);

        bot.sendMessage(locationInfoMessage);
    }

    private String getLocationInfoText(Location location) {
        return String.format("""
                        *Локация в путешествии "%s"*
                        
                        *🌍️ Название:* %s
                        *🛫 Время отправления:* %s
                        *📥 Время прибытия:* %s
                        *🗺 Улица:* ___%s___
                        *🕒 Создано:* %s
                        """,
                location.getTravel().getName(),
                location.getName(),
                formatInstant(location.getStartTime()),
                formatInstant(location.getEndTime()),
                location.getStreet(),
                formatInstant(location.getCreatedAt())
        );
    }

    private String formatInstant(Instant instant) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return instant.atZone(ZoneId.systemDefault()).format(dateTimeFormatter);
    }

    private void getChoiceOfLocations(TravelAgentBot bot, long chatId, long travelId, List<String> locations) {
        StringBuilder textBuilder = new StringBuilder();

        List<Long> locationIds = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            textBuilder.append(i + 1).append(". ").append(locations.get(i));
            locationIds.add(cacheManager.saveLocation(locations.get(i)));

            if (i != locations.size() - 1) {
                textBuilder.append("\n");
            }
        }

        var choiceOfCitiesMessage = SendMessage.builder()
                .chatId(chatId)
                .text("*Выберите локацию из предложенных:*\n\n" + textBuilder)
                .replyMarkup(generateChoiceOfLocationsKeyboardMarkup(locations, locationIds))
                .build();
        choiceOfCitiesMessage.enableMarkdown(true);

        bot.sendMessage(choiceOfCitiesMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.CHOICE_OF_LOCATIONS)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );
    }

    public void chooseLocation(TravelAgentBot bot, CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String locationId = "location:" + callbackQuery.getData().split("_")[2];
        Optional<String> locationNameOptional = cacheManager.fetch(locationId, String.class);

        List<String> cacheData = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class).get().getData();
        long travelId = Long.parseLong(cacheData.get(0));

        if (locationNameOptional.isEmpty()) {
            sendExpiredOperationMessage(bot, chatId);
        } else {
            enterTime(bot, chatId, List.of(String.valueOf(travelId), locationNameOptional.get()), true);
        }
    }

    private void sendExpiredOperationMessage(TravelAgentBot bot, long chatId) {
        var expiredOperationMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Извините, время действия этой операции истекло. Перезапустите ее и попробуйте снова")
                .build();
        bot.sendMessage(expiredOperationMessage);
    }

    public void getTravelLocations(TravelAgentBot bot, CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        List<Location> locations = locationService.getTravelLocations(travelId);

        StringBuilder locationsInfoBuilder = new StringBuilder();
        for (int i = 0; i < locations.size(); i++) {
            Location location = locations.get(i);
            String locationInfo = String.format(
                    "*%s* (%s - %s)%n",
                    location.getName(),
                    formatInstant(location.getStartTime()),
                    formatInstant(location.getEndTime())
            );

            locationsInfoBuilder.append(i + 1).append(". ").append(locationInfo);
        }

        var newMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(locationsInfoBuilder.toString())
                .replyMarkup(generateTravelLocationsKeyboardMarkup())
                .build();
        newMessage.enableMarkdown(true);

        bot.editMessage(newMessage);
    }
}
