package com.halcyon.travelagent.bot;

import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.config.Credentials;
import com.halcyon.travelagent.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
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

    public Message sendMessage(SendMessage sendMessage) {
        try {
            return telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message.");
        }

        return null;
    }

    public void editMessage(EditMessageText editMessageText) {
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message.");
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();

        try {
            telegramClient.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to delete message.");
        }
    }

    public Message sendPhoto(SendPhoto sendPhoto) {
        try {
            return telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Failed to send photo.");
        }

        return null;
    }

    public Message sendFile(SendDocument sendDocument) {
        try {
            return telegramClient.execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("Failed to send file.");
        }

        return null;
    }

    public Message sendVoice(SendVoice sendVoice) {
        try {
            return telegramClient.execute(sendVoice);
        } catch (TelegramApiException e) {
            log.error("Failed to send voice.");
        }

        return null;
    }

    public String getTravelInfoText(Travel travel) {
        return String.format("""
                        *🌍️ Название:* %s
                        *📖 Описание:* ___%s___
                        *🕒 Создано:* %s
                        """,
                travel.getName(),
                travel.getDescription(),
                travel.getCreatedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );
    }

    public void editMessageToUserTravels(long chatId, int messageId, List<Travel> userTravels) {
        var userTravelsMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        userTravelsMessage.enableMarkdown(true);

        editMessage(userTravelsMessage);
    }

    public void sendUserTravelsMessage(long chatId, List<Travel> userTravels) {
        var userTravelsMessage = SendMessage.builder()
                .chatId(chatId)
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        userTravelsMessage.enableMarkdown(true);

        sendMessage(userTravelsMessage);
    }

    public void sendInvalidDataMessage(Message message, String errorText) {
        var errorMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text(errorText)
                .replyToMessageId(message.getMessageId())
                .build();

        errorMessage.enableMarkdown(true);
        sendMessage(errorMessage);
    }

    public void sendEnterDataMessage(CallbackQuery callbackQuery, String data) {
        var message = SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text("Пожалуйста, введите " + data + " для путешествия")
                .build();

        sendMessage(message);
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

    public void sendNoteInfo(long chatId, int messageId, Note note) {
        String noteInfoText = String.format(
                """
                Заметка в путешествии "%s"
                
                *🏷 %s*
                _%s_
                """,
                note.getTravel().getName(),
                note.getName(),
                note.getCreatedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        var noteInfoMessage = SendMessage.builder()
                .chatId(chatId)
                .text(noteInfoText)
                .build();
        noteInfoMessage.enableMarkdown(true);

        switch (note.getType()) {
            case TEXT -> {
                var noteInfoEditMessage = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(getTextNoteInfo(note))
                        .replyMarkup(generateNoteInfoKeyboardMarkup(note.getId(), -1))
                        .build();
                noteInfoEditMessage.enableMarkdown(true);

                editMessage(noteInfoEditMessage);
            }

            case PHOTO -> {
                deleteMessage(chatId, messageId);

                var notePhoto = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(note.getFileId()))
                        .build();

                Message sentMessage = sendPhoto(notePhoto);

                noteInfoMessage.setReplyMarkup(generateNoteInfoKeyboardMarkup(note.getId(), sentMessage.getMessageId()));
                sendMessage(noteInfoMessage);
            }

            case FILE -> {
                deleteMessage(chatId, messageId);

                var noteFile = SendDocument.builder()
                        .chatId(chatId)
                        .document(new InputFile(note.getFileId()))
                        .build();

                Message sentMessage = sendFile(noteFile);

                noteInfoMessage.setReplyMarkup(generateNoteInfoKeyboardMarkup(note.getId(), sentMessage.getMessageId()));
                sendMessage(noteInfoMessage);
            }

            default -> {
                deleteMessage(chatId, messageId);

                var noteVoice = SendVoice.builder()
                        .chatId(chatId)
                        .voice(new InputFile(note.getFileId()))
                        .build();

                Message sentMessage = sendVoice(noteVoice);

                noteInfoMessage.setReplyMarkup(generateNoteInfoKeyboardMarkup(note.getId(), sentMessage.getMessageId()));
                sendMessage(noteInfoMessage);
            }
        }
    }

    private String getTextNoteInfo(Note note) {
        return String.format(
                """
                Заметка в путешествии "%s"
                *🏷 %s*

                %s
                
                _%s_
                """,
                note.getTravel().getName(),
                note.getName(),
                note.getText(),
                note.getCreatedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    }
}
