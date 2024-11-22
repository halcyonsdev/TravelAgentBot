package com.halcyon.travelagent;

import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.config.Credentials;
import com.halcyon.travelagent.controller.CommandController;
import com.halcyon.travelagent.controller.LocationController;
import com.halcyon.travelagent.controller.TravelController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelAgentBot implements LongPollingSingleThreadUpdateConsumer {
    private final CommandController commandController;
    private final TravelController travelController;
    private final LocationController locationController;
    private final CacheManager cacheManager;

    private final TelegramClient telegramClient = new OkHttpTelegramClient(Credentials.getBotToken());

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();

            if (message.getText().equals("/start")) {
                commandController.handleStartCommand(this, message.getChatId());
            } else if (message.getText().charAt(0) == '/') {
                    commandController.handleUnknownCommand(this, message);
            } else {
                processStatus(message);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();

            switch (callbackData) {
                case "get_travels" -> travelController.getTravels(this, callbackQuery);
                case "create_travel" -> travelController.createTravel(this, callbackQuery);

                case "back" -> commandController.handleBackCommand(this, callbackQuery);

                default -> {
                    if (callbackData.startsWith("info_travel_")) {
                        travelController.getTravel(this, callbackQuery);
                    } else if (callbackData.startsWith("change_travel_name_")) {
                        travelController.enterNewTravelName(this, callbackQuery);
                    } else if (callbackData.startsWith("change_travel_description_")) {
                        travelController.enterNewTravelDescription(this, callbackQuery);
                    } else if (callbackData.startsWith("change_travel_")) {
                        travelController.changeTravel(this, callbackQuery);
                    } else if (callbackData.startsWith("delete_travel_")) {
                        travelController.deleteTravel(this, callbackQuery);
                    } else if (callbackData.startsWith("add_travel_location")) {
                        locationController.enterLocationCity(this, callbackQuery);
                    } else if (callbackData.startsWith("choose_location_")) {
                        locationController.chooseLocation(this, callbackQuery);
                    } else if (callbackData.startsWith("locations_travel_")) {
                        locationController.getTravelLocations(this, callbackQuery);
                    }
                }
            }
        }
    }

    private void processStatus(Message message) {
        Optional<ChatStatus> chatStatusOptional = cacheManager.fetch(String.valueOf(message.getChatId()), ChatStatus.class);

        if (chatStatusOptional.isEmpty()) {
            return;
        }

        ChatStatus chatStatus = chatStatusOptional.get();

        switch (chatStatus.getType()) {
            case TRAVEL_NAME -> travelController.changeTravelName(this, message, Long.parseLong(chatStatus.getData().get(0)));
            case TRAVEL_DESCRIPTION -> travelController.changeTravelDescription(this, message, Long.parseLong(chatStatus.getData().get(0)));
            case LOCATION_CITY -> locationController.createLocation(this, message, Long.parseLong(chatStatus.getData().get(0)));
            case LOCATION_START_TIME -> {
                long travelId = Long.parseLong(chatStatus.getData().get(0));
                String locationName = chatStatus.getData().get(1);

                locationController.setLocationStartTime(this, message, travelId, locationName);
            }
            case LOCATION_END_TIME -> {
                long travelId = Long.parseLong(chatStatus.getData().get(0));
                String locationName = chatStatus.getData().get(1);
                String locationStartTime = chatStatus.getData().get(2);

                locationController.setLocationEndTime(this, message, travelId, locationName, locationStartTime);
            }
        }
    }

    public void sendMessage(SendMessage sendMessage) {
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message.");
            e.printStackTrace();
        }
    }

    public void editMessage(EditMessageText editMessageText) {
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message.");
        }
    }
}
