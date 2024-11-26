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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.*;

@Controller
@RequiredArgsConstructor
public class EditLocationController {
    private final LocationService locationService;
    private final CacheManager cacheManager;
    private final GeoapifyAPI geoapifyAPI;
    private final BotMessageHelper botMessageHelper;

    private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm";

    public void enterNewLocationName(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String locationId = callbackQuery.getData().split("_")[3];

        botMessageHelper.sendEnterCityMessage(chatId, true);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.CHANGE_LOCATION_CITY)
                        .data(List.of(locationId))
                        .build()
        );
    }

    public void changeLocationName(Message message, long locationId) {
        long chatId = message.getChatId();
        Optional<List<String>> locationsOptional = geoapifyAPI.getLocations(message.getText());

        if (locationsOptional.isPresent()) {
            List<String> locations = locationsOptional.get();

            if (locations.size() == 1) {
                Location location = locationService.changeName(locationId, locations.get(0));
                botMessageHelper.sendLocationInfoMessage(chatId, location);
                cacheManager.remove(String.valueOf(chatId));
            } else {
                botMessageHelper.sendChoiceOfLocationsMessage(chatId, locations);
            }
        } else {
            botMessageHelper.sendEnterCityMessage(chatId, true);
        }
    }

    public void getLocation(CallbackQuery callbackQuery) {
        long locationId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Location location = locationService.findById(locationId);

        var locationInfoMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(botMessageHelper.getLocationInfoText(location))
                .replyMarkup(generateLocationInfoKeyboardMarkup(location.getId()))
                .build();
        locationInfoMessage.enableMarkdown(true);

        botMessageHelper.editMessage(locationInfoMessage);
    }

    public void enterLocationStreet(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String locationId = callbackQuery.getData().split("_")[3];

        sendEnterStreetMessage(chatId);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.LOCATION_STREET)
                        .data(List.of(locationId))
                        .build()
        );
    }

    private void sendEnterStreetMessage(long chatId) {
        var enterStreetMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите название улицы")
                .build();

        botMessageHelper.sendMessage(enterStreetMessage);
    }

    public void changeLocationStreet(Message message, long locationId) {
        long chatId = message.getChatId();
        Location location = locationService.findById(locationId);
        Optional<String> streetOptional = geoapifyAPI.getStreet(message.getText(), location.getName());

        if (streetOptional.isPresent()) {
            location.setStreet(streetOptional.get());
            location = locationService.save(location);

            botMessageHelper.sendLocationInfoMessage(chatId, location);
            cacheManager.remove(String.valueOf(chatId));
        } else {
            sendEnterStreetMessage(chatId);
        }
    }

    public void enterNewLocationTime(CallbackQuery callbackQuery, boolean isStart) {
        long chatId = callbackQuery.getMessage().getChatId();
        String locationId = callbackQuery.getData().split("_")[3];

        botMessageHelper.sendEnterTimeMessage(chatId, isStart, true);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(isStart ? ChatStatusType.CHANGE_LOCATION_START_TIME : ChatStatusType.CHANGE_LOCATION_END_TIME)
                        .data(List.of(locationId))
                        .build()
        );
    }

    public void changeLocationTime(Message message, long locationId, boolean isStart) {
        long chatId = message.getChatId();

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(message.getText(), DateTimeFormatter.ofPattern(DATE_PATTERN));
            Instant time = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

            Location location;
            if (isStart) {
                location = locationService.changeStartTime(locationId, time);
            } else {
                location = locationService.changeEndTime(locationId, time);
            }

            botMessageHelper.sendLocationInfoMessage(chatId, location);
        } catch (DateTimeParseException e) {
            botMessageHelper.sendErrorTimeMessage(chatId, isStart, true);
        }
    }

    public void deleteLocation(CallbackQuery callbackQuery) {
        long locationId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        long travelId = locationService.deleteLocationAndGetTravelId(locationId);

        botMessageHelper.getTravelLocations(callbackQuery, locationService.getTravelLocations(travelId));
    }
}
