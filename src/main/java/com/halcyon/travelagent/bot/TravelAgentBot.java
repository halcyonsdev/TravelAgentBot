package com.halcyon.travelagent.bot;

import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.controller.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelAgentBot implements LongPollingSingleThreadUpdateConsumer {
    private final CommandController commandController;
    private final CreateTravelController createTravelController;
    private final EditTravelController editTravelController;
    private final CreateLocationController createLocationController;
    private final EditLocationController editLocationController;
    private final CreateRouteController createRouteController;
    private final EditRouteController editRouteController;
    private final CacheManager cacheManager;

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            processMessage(update);
        } else if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        }
    }

    private void processMessage(Update update) {
        Message message = update.getMessage();

        if (message.getText().equals("/start")) {
            commandController.handleStartCommand(message.getChatId());
        } else if (message.getText().charAt(0) == '/') {
            commandController.handleUnknownCommand(message);
        } else {
            processStatus(message);
        }
    }

    private void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();

        switch (callbackData) {
            case "get_travels" -> createTravelController.editMessageToUserTravels(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
            case "create_travel" -> createTravelController.enterTravelName(callbackQuery);

            case "back" -> commandController.handleBackCommand(callbackQuery);

            default -> handleCommands(callbackQuery);
        }
    }

    private void handleCommands(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        if (callbackData.startsWith("info_travel_")) {
            createTravelController.getTravel(callbackQuery);
        } else if (callbackData.startsWith("change_travel_name_")) {
            editTravelController.enterNewTravelName(callbackQuery);
        } else if (callbackData.startsWith("change_travel_description_")) {
            editTravelController.enterNewTravelDescription(callbackQuery);
        } else if (callbackData.startsWith("change_travel_")) {
            editTravelController.changeTravel(callbackQuery);
        } else if (callbackData.startsWith("delete_travel_")) {
            editTravelController.deleteTravel(callbackQuery);
        } else if (callbackData.startsWith("add_travel_location")) {
            createLocationController.enterLocationCity(callbackQuery);
        } else if (callbackData.startsWith("choose_location_")) {
            createLocationController.chooseLocation(callbackQuery);
        } else if (callbackData.startsWith("locations_travel_")) {
            createLocationController.getTravelLocations(callbackQuery);
        } else if (callbackData.startsWith("get_location_")) {
            editLocationController.getLocation(callbackQuery);
        } else if (callbackData.startsWith("change_location_name_")) {
            editLocationController.enterNewLocationName(callbackQuery);
        } else if (callbackData.startsWith("change_location_street_")) {
            editLocationController.enterLocationStreet(callbackQuery);
        } else if (callbackData.startsWith("change_location_start_")) {
            editLocationController.enterNewLocationTime(callbackQuery, true);
        } else if (callbackData.startsWith("change_location_end_")) {
            editLocationController.enterNewLocationTime(callbackQuery, false);
        } else if (callbackData.startsWith("delete_location_")) {
            editLocationController.deleteLocation(callbackQuery);
        } else if (callbackData.startsWith("build_route_")) {
            createRouteController.chooseStartPoint(callbackQuery);
        } else if (callbackData.startsWith("route_start_location_")) {
            createRouteController.chooseDestinationPoint(callbackQuery);
        } else if (callbackData.startsWith("route_destination_location_")) {
            createRouteController.enterRouteName(callbackQuery);
        } else if (callbackData.startsWith("routes_travel_")) {
            createRouteController.getTravelRoutes(callbackQuery);
        } else if (callbackData.startsWith("info_route_")) {
            createRouteController.getRoute(callbackQuery);
        } else if (callbackData.startsWith("delete_route_point_")) {
            editRouteController.choosePointForDeleting(callbackQuery);
        } else if (callbackData.startsWith("delete_route_")) {
            editRouteController.deleteRoute(callbackQuery);
        } else if (callbackData.startsWith("add_point_route_")) {
            editRouteController.sendChoosePointLocationMessage(callbackQuery);
        } else if (callbackData.startsWith("add_point_location_")) {
            editRouteController.choosePointLocation(callbackQuery);
        } else if (callbackData.startsWith("choose_add_point_")) {
            long routePointId = Long.parseLong(callbackData.split("_")[3]);
            editRouteController.createRouteWithNewPoint(callbackQuery, routePointId);
        } else if (callbackData.startsWith("new_start_point")) {
            editRouteController.createRouteWithNewPoint(callbackQuery, -1);
        } else if (callbackData.startsWith("change_route_name_")) {
            editRouteController.sendEnterNewRouteNameMessage(callbackQuery);
        } else if (callbackData.startsWith("choose_delete_point_")) {
            editRouteController.deleteRoutePoint(callbackQuery);
        }
    }

    private void processStatus(Message message) {
        Optional<ChatStatus> chatStatusOptional = cacheManager.fetch(String.valueOf(message.getChatId()), ChatStatus.class);

        if (chatStatusOptional.isEmpty()) {
            return;
        }

        ChatStatus chatStatus = chatStatusOptional.get();

        switch (chatStatus.getType()) {
            case TRAVEL_NAME -> createTravelController.createTravel(message);
            case TRAVEL_DESCRIPTION -> editTravelController.changeTravelDescription(message, Long.parseLong(chatStatus.getData().get(0)));
            case CHANGE_TRAVEL_NAME -> editTravelController.changeTravelName(message, Long.parseLong(chatStatus.getData().get(0)));

            case LOCATION_CITY -> createLocationController.createLocation(message, Long.parseLong(chatStatus.getData().get(0)));
            case LOCATION_STREET -> editLocationController.changeLocationStreet(message, Long.parseLong(chatStatus.getData().get(0)));
            case LOCATION_START_TIME -> {
                long travelId = Long.parseLong(chatStatus.getData().get(0));
                String locationName = chatStatus.getData().get(1);

                createLocationController.setLocationStartTime(message, travelId, locationName);
            }
            case LOCATION_END_TIME -> {
                long travelId = Long.parseLong(chatStatus.getData().get(0));
                String locationName = chatStatus.getData().get(1);
                String locationStartTime = chatStatus.getData().get(2);

                createLocationController.setLocationEndTime(message, travelId, locationName, locationStartTime);
            }
            case CHANGE_LOCATION_CITY -> editLocationController.changeLocationName(message, Long.parseLong(chatStatus.getData().get(0)));
            case CHANGE_LOCATION_START_TIME -> editLocationController.changeLocationTime(message, Long.parseLong(chatStatus.getData().get(0)), true);
            case CHANGE_LOCATION_END_TIME -> editLocationController.changeLocationTime(message, Long.parseLong(chatStatus.getData().get(0)), false);

            case ROUTE_NAME -> createRouteController.createRoute(message, chatStatus.getData());
            case CHANGE_ROUTE_NAME -> editRouteController.changeRouteName(message, Long.parseLong(chatStatus.getData().get(0)));
        }
    }
}
