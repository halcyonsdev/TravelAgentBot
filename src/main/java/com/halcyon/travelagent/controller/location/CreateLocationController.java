package com.halcyon.travelagent.controller.location;

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
import java.util.ArrayList;
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

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendEnterCityMessage(chatId, false);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.LOCATION_CITY)
                        .data(List.of(String.valueOf(travelId), String.valueOf(sentMessage.getMessageId())))
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

    public void createLocation(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        Optional<List<String>> locationsOptional = geoapifyAPI.getLocations(message.getText());

        long travelId = Long.parseLong(cachedData.get(0));
        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        if (locationsOptional.isPresent()) {
            List<String> locations = locationsOptional.get();

            List<String> data = new ArrayList<>();
            data.add(String.valueOf(travelId));
            data.add(locations.get(0));

            if (locations.size() == 1) {
                enterTime(chatId, data, true);
            } else {
                getChoiceOfLocations(chatId, travelId, locations);
            }
        } else {
            botMessageHelper.sendEnterCityMessage(chatId, false);
        }
    }

    public void enterTime(long chatId, List<String> data, boolean isStart) {
        Message sentMessage = botMessageHelper.sendEnterTimeMessage(chatId, isStart, false);
        data.add(String.valueOf(sentMessage.getMessageId()));

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(isStart ? ChatStatusType.LOCATION_START_TIME : ChatStatusType.LOCATION_END_TIME)
                        .data(data)
                        .build()
        );
    }

    public void setLocationStartTime(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        long travelId = Long.parseLong(cachedData.get(0));
        String locationName = cachedData.get(1);
        int toDeleteMessageId = Integer.parseInt(cachedData.get(2));

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(message.getText(), DateTimeFormatter.ofPattern(DATE_PATTERN));
            localDateTime.atZone(ZoneId.systemDefault()).toInstant();

            List<String> data = new ArrayList<>();
            data.add(String.valueOf(travelId));
            data.add(locationName);
            data.add(message.getText());

            enterTime(chatId, data, false);
        } catch (DateTimeParseException e) {
            botMessageHelper.sendErrorTimeMessage(chatId, true, false);
        }
    }

    public void setLocationEndTime(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        long travelId = Long.parseLong(cachedData.get(0));
        String locationName = cachedData.get(1);
        String locationStartTime = cachedData.get(2);
        int toDeleteMessageId = Integer.parseInt(cachedData.get(3));

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

            LocalDateTime endDateTime = LocalDateTime.parse(message.getText(), dateTimeFormatter);
            Instant endTime = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

            LocalDateTime startDateTime = LocalDateTime.parse(locationStartTime, dateTimeFormatter);
            Instant startTime = startDateTime.atZone(ZoneId.systemDefault()).toInstant();

            Location location = locationService.createLocation(locationName, startTime, endTime, travelId);

            botMessageHelper.sendLocationInfoMessage(chatId, location);
            cacheManager.remove(String.valueOf(chatId));
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

        Optional<ChatStatus> chatStatusOptional = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class);

        if (chatStatusOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        ChatStatus chatStatus = chatStatusOptional.get();
        List<String> cachedData = chatStatus.getData();

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

        if (locationNameOptional.isEmpty()) {
            sendExpiredOperationMessage(chatId);
        } else if (chatStatus.getType() == ChatStatusType.CHANGE_LOCATION_CITY) {
            long locationId = Long.parseLong(cachedData.get(0));
            Location location = locationService.changeName(locationId, locationNameOptional.get());

            botMessageHelper.sendLocationInfoMessage(chatId, location);
            cacheManager.remove(String.valueOf(chatId));
        } else {
            long travelId = Long.parseLong(cachedData.get(0));

            List<String> data = new ArrayList<>();
            data.add(String.valueOf(travelId));
            data.add(locationNameOptional.get());

            enterTime(chatId, data, true);
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
