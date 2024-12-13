package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.api.geoapify.GeoapifyAPI;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CreateLocationController {
    private final LocationService locationService;
    private final CacheManager cacheManager;
    private final GeoapifyAPI geoapifyAPI;
    private final BotMessageHelper botMessageHelper;

    private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm";

    public void enterLocationCity(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        if (locationService.getTravelLocationsCount(travelId) >= 20) {
            sendExceededLimitMessage(chatId);
            return;
        }

        botMessageHelper.sendEnterCityMessage(chatId, false);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.LOCATION_CITY)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );
    }

    private void sendExceededLimitMessage(long chatId) {
        var exceededLimitMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Вы не можете создать больше 20 локаций в одном путешествии")
                .build();

        botMessageHelper.sendMessage(exceededLimitMessage);
    }

    public void createLocation(Message message, long travelId) {
        long chatId = message.getChatId();
        Optional<List<String>> locationsOptional = geoapifyAPI.getLocations(message.getText());

        if (locationsOptional.isPresent()) {
            List<String> locations = locationsOptional.get();

            if (locations.size() == 1) {
                enterTime(chatId, List.of(String.valueOf(travelId), locations.get(0)), true);
            } else {
                getChoiceOfLocations(chatId, travelId, locations);
            }
        } else {
            botMessageHelper.sendEnterCityMessage(chatId, false);
        }
    }

    public void enterTime(long chatId, List<String> data, boolean isStart) {
        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(isStart ? ChatStatusType.LOCATION_START_TIME : ChatStatusType.LOCATION_END_TIME)
                        .data(data)
                        .build()
        );

        botMessageHelper.sendEnterTimeMessage(chatId, isStart, false);
    }

    public void setLocationStartTime(Message message, long travelId, String locationName) {
        long chatId = message.getChatId();

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(message.getText(), DateTimeFormatter.ofPattern(DATE_PATTERN));
            localDateTime.atZone(ZoneId.systemDefault()).toInstant();

            List<String> data = List.of(String.valueOf(travelId), locationName, message.getText());
            enterTime(chatId, data, false);
        } catch (DateTimeParseException e) {
            botMessageHelper.sendErrorTimeMessage(chatId, true, false);
        }
    }

    public void setLocationEndTime(Message message, long travelId, String locationName, String locationStartTime) {
        long chatId = message.getChatId();

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

            LocalDateTime endDateTime = LocalDateTime.parse(message.getText(), dateTimeFormatter);
            Instant endTime = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

            LocalDateTime startDateTime = LocalDateTime.parse(locationStartTime, dateTimeFormatter);
            Instant startTime = startDateTime.atZone(ZoneId.systemDefault()).toInstant();

            Location location = locationService.createLocation(locationName, startTime, endTime, travelId);
            cacheManager.remove(String.valueOf(chatId));

            botMessageHelper.sendLocationInfoMessage(chatId, location);
        } catch (DateTimeParseException e) {
            botMessageHelper.sendErrorTimeMessage(chatId, false, false);
        }
    }

    private void getChoiceOfLocations(long chatId, long travelId, List<String> locations) {
        botMessageHelper.sendChoiceOfLocationsMessage(chatId, locations);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.CHOICE_OF_LOCATIONS)
                        .data(List.of(String.valueOf(travelId)))
                        .build()
        );
    }

    public void chooseLocation(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String cachedLocationId = "location:" + callbackQuery.getData().split("_")[2];
        Optional<String> locationNameOptional = cacheManager.fetch(cachedLocationId, String.class);

        ChatStatus chatStatus = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class).get();
        List<String> cacheData = chatStatus.getData();

        if (locationNameOptional.isEmpty()) {
            sendExpiredOperationMessage(chatId);
        } else if (chatStatus.getType() == ChatStatusType.CHANGE_LOCATION_CITY) {
            long locationId = Long.parseLong(cacheData.get(0));
            Location location = locationService.changeName(locationId, locationNameOptional.get());

            botMessageHelper.sendLocationInfoMessage(chatId, location);
            cacheManager.remove(String.valueOf(chatId));
        } else {
            long travelId = Long.parseLong(cacheData.get(0));
            enterTime(chatId, List.of(String.valueOf(travelId), locationNameOptional.get()), true);
        }
    }

    private void sendExpiredOperationMessage(long chatId) {
        var expiredOperationMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Извините, время действия этой операции истекло. Перезапустите ее и попробуйте снова")
                .build();
        botMessageHelper.sendMessage(expiredOperationMessage);
    }

    public void getTravelLocations(CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        List<Location> locations = locationService.getTravelLocations(travelId);

        botMessageHelper.getTravelLocations(callbackQuery, locations, travelId);
    }
}
