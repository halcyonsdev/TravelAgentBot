package com.halcyon.travelagent.bot;

import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.config.Credentials;
import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.entity.Route;
import com.halcyon.travelagent.entity.RoutePoint;
import com.halcyon.travelagent.entity.Travel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotMessageHelper {
    private final CacheManager cacheManager;

    private final TelegramClient telegramClient = new OkHttpTelegramClient(Credentials.getBotToken());

    private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm";

    public void sendMessage(SendMessage sendMessage) {
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message.");
        }
    }

    public void editMessage(EditMessageText editMessageText) {
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message.");
        }
    }

    public void sendPhoto(SendPhoto sendPhoto) {
        try {
            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Failed to send photo.");
        }
    }

    public void sendEnterCityMessage(long chatId, boolean isForChange) {
        var enterCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите " + (isForChange ? "новое " : "") + "название города, который хотите посетить")
                .build();

        sendMessage(enterCityMessage);
    }

    public void sendEnterTimeMessage(long chatId, boolean isStart, boolean isForChange) {
        String text = String.format(
                "Пожалуйста, введите%s время %s (в формате %s)",
                (isForChange ? " новое" : ""),
                (isStart ? "отправления" : "прибытия"),
                DATE_PATTERN
        );

        var enterTimeMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        sendMessage(enterTimeMessage);
    }

    public void sendErrorTimeMessage(long chatId, boolean isStart, boolean isForChange) {
        String text = String.format(
                "Введенные данные не соответствуют формату %s. Пожалуйста, введите%s время %s по указанному формату.",
                DATE_PATTERN,
                (isForChange ? " новое" : ""),
                (isStart ? "отправления" : "прибытия")
        );

        var errorTimeMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        sendMessage(errorTimeMessage);
    }

    public void sendLocationInfoMessage(long chatId, Location location) {
        var locationInfoMessage = SendMessage.builder()
                .chatId(chatId)
                .text(getLocationInfoText(location))
                .replyMarkup(generateLocationInfoKeyboardMarkup(location.getId()))
                .build();
        locationInfoMessage.enableMarkdown(true);

        sendMessage(locationInfoMessage);
    }

    public String getLocationInfoText(Location location) {
        return String.format("""
                        *Локация в путешествии "%s"*
                        
                        *🌍️ Название:* %s
                        *🛫 Время отправления:* %s
                        *🛬 Время прибытия:* %s
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

    public String formatInstant(Instant instant) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return instant.atZone(ZoneId.systemDefault()).format(dateTimeFormatter);
    }

    public void sendChoiceOfLocationsMessage(long chatId, List<String> locations) {
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

        sendMessage(choiceOfCitiesMessage);
    }

    public void getTravelLocations(CallbackQuery callbackQuery, List<Location> locations, long travelId) {
        String text = "___Нет созданных локаций для данного путешествия___";

        if (!locations.isEmpty()) {
            text = getTravelLocationsText(locations);
        }

        var travelLocationsMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(text)
                .replyMarkup(generateGetTravelLocationsKeyboardMarkup(locations, travelId))
                .build();
        travelLocationsMessage.enableMarkdown(true);

        editMessage(travelLocationsMessage);
    }

    public String getTravelLocationsText(List<Location> locations) {
        StringBuilder locationsInfoBuilder = new StringBuilder();

        for (int i = 0; i < locations.size(); i++) {
            Location location = locations.get(i);
            String locationInfo = String.format(
                    "*%s%s* (%s - %s)%n",
                    location.getName(),
                    (location.getStreet().equals("отсутствует") ? "" : ", " + location.getStreet()),
                    formatInstant(location.getStartTime()),
                    formatInstant(location.getEndTime())
            );

            locationsInfoBuilder.append(i + 1).append(". ").append(locationInfo);
        }

        return locationsInfoBuilder.toString();
    }

    public void sendTravelRoutesMessage(long chatId, int messageId, Travel travel, List<Route> travelRoutes) {


        var travelRoutesMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.format("*Маршруты в путешествии \"%s\"*", travel.getName()))
                .replyMarkup(generateTravelRoutesInlineKeyboard(travelRoutes, travel.getId()))
                .build();
        travelRoutesMessage.enableMarkdown(true);

        editMessage(travelRoutesMessage);
    }

    public void sendErrorMessage(long chatId) {
        var errorMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Что-то пошло не так. Пожалуйста, попробуйте попытку позже.")
                .build();

        sendMessage(errorMessage);
    }

    public void sendRoute(long chatId, InputFile routeImage, Route route) {
        var routePhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(routeImage)
                .build();

        var routeInfoMessage = SendMessage.builder()
                .chatId(chatId)
                .text(getRouteInfo(route))
                .replyMarkup(generateRouteInfoInlineKeyboard(route.getId()))
                .build();
        routeInfoMessage.enableMarkdown(true);

        sendPhoto(routePhoto);
        sendMessage(routeInfoMessage);
    }

    private String getRouteInfo(Route route) {
        StringBuilder routeInfo = new StringBuilder(
                String.format("*Название:* \"%s\" %n%n*Точки маршрута:* %n", route.getName())
        );

        RoutePoint routePoint = route.getStartPoint();

        int number = 1;
        while (routePoint != null) {
            routeInfo.append(number++).append(". ").append(routePoint.getName()).append("\n");
            routePoint = routePoint.getNextPoint();
        }

        return routeInfo.toString();
    }

    public void sendInvalidRouteNameMessage(long chatId) {
        var invalidNameMessage = SendMessage.builder()
                .chatId(chatId)
                .text("*Длина названия маршрута не должна превышать 100 символов!* Пожалуйта, введите название снова")
                .build();

        sendMessage(invalidNameMessage);
    }
}
