package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.api.geoapify.GeoapifyAPI;
import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.entity.Route;
import com.halcyon.travelagent.entity.RoutePoint;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.LocationService;
import com.halcyon.travelagent.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.generateAddPointLocationsKeyboardMarkup;
import static com.halcyon.travelagent.util.KeyboardUtils.generateChooseRoutePointKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class EditRouteController {
    private final BotMessageHelper botMessageHelper;
    private final CacheManager cacheManager;
    private final RouteService routeService;
    private final LocationService locationService;
    private final GeoapifyAPI geoapifyAPI;

    public void sendChoosePointLocationMessage(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long routeId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        Route route = routeService.findById(routeId);
        List<Location> travelLocations = locationService.getTravelLocations(route.getTravel().getId());
        StringBuilder locationsText = new StringBuilder("*Выберите локацию для новой точки в маршруте:*\n\n");

        int destinationLocationIndex = 0;
        int number = 1;

        for (int i = 0; i < travelLocations.size(); i++) {
            Location location = travelLocations.get(i);

            if (location.getName().equals(route.getDestinationPoint().getName())) {
                destinationLocationIndex = i;
                continue;
            }

            locationsText.append(String.format("%s. %s%n", number++, location.getName()));
        }

        travelLocations.remove(destinationLocationIndex);

        var choosePointLocationMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(locationsText.toString())
                .replyMarkup(generateAddPointLocationsKeyboardMarkup(travelLocations))
                .build();
        choosePointLocationMessage.enableMarkdown(true);

        botMessageHelper.editMessage(choosePointLocationMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.ROUTE_POINT)
                        .data(List.of(String.valueOf(routeId)))
                        .build()
        );
    }

    public void choosePointLocation(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        long locationId = Long.parseLong(callbackQuery.getData().split("_")[3]);
        Optional<ChatStatus> chatStatusOptional = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class);

        if (chatStatusOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        ChatStatus chatStatus = chatStatusOptional.get();
        long routeId = Long.parseLong(chatStatus.getData().get(0));
        Location location = locationService.findById(locationId);
        Route route = routeService.findById(routeId);

        StringBuilder routePointsText = new StringBuilder("*Выбери, после какой точки добавить новую:*\n\n");
        RoutePoint currentPoint = route.getStartPoint();
        List<RoutePoint> routePoints = new ArrayList<>();

        int number = 1;
        while (currentPoint != null) {
            routePointsText.append(String.format("%s. %s%n", number++, currentPoint.getName()));
            routePoints.add(currentPoint);

            currentPoint = currentPoint.getNextPoint();
        }

        var chooseRouteMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(routePointsText.toString())
                .replyMarkup(generateChooseRoutePointKeyboardMarkup(routePoints))
                .build();
        chooseRouteMessage.enableMarkdown(true);

        botMessageHelper.editMessage(chooseRouteMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(chatStatus.getType())
                        .data(List.of(String.valueOf(routeId), location.getName()))
                        .build()
        );
    }

    public void createRouteWithNewPoint(CallbackQuery callbackQuery, long routePointId) {
        long chatId = callbackQuery.getMessage().getChatId();
        Optional<ChatStatus> chatStatusOptional = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class);

        if (chatStatusOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        ChatStatus chatStatus = chatStatusOptional.get();
        String newPointName = chatStatus.getData().get(1);
        long routeId = Long.parseLong(chatStatus.getData().get(0));

        Route route = routeService.findById(routeId);
        Optional<String> routeImageUrlOptional = geoapifyAPI.getUpdatedRouteImageUrl(route, routePointId, newPointName);

        if (routeImageUrlOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        String routeImageUrl = routeImageUrlOptional.get();

        Optional<InputFile> routeImageFileOptional = geoapifyAPI.getRouteImageFile(routeImageUrl);

        if (routeImageFileOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        if (routePointId != -1) {
            route = routeService.addNewRoutePoint(routeId, routePointId, newPointName, routeImageUrl);
        } else {
            route = routeService.addNewStartRoutePoint(routeId, newPointName, routeImageUrl);
        }

        botMessageHelper.sendRoute(chatId, routeImageFileOptional.get(), route);
        cacheManager.remove(String.valueOf(chatId));
    }

    public void sendEnterNewRouteNameMessage(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long routeId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        var enterNewNameMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Введите новое название для маршрута")
                .build();
        enterNewNameMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(enterNewNameMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.CHANGE_ROUTE_NAME)
                        .data(List.of(String.valueOf(routeId)))
                        .build()
        );
    }

    public void changeRouteName(Message message, long routeId) {
        long chatId = message.getChatId();

        if (message.getText().length() > 100) {
            botMessageHelper.sendInvalidRouteNameMessage(chatId);
            return;
        }

        Route route = routeService.changeName(routeId, message.getText());
        Optional<InputFile> routeImageFileOptional = geoapifyAPI.getRouteImageFile(route.getApiMapUrl());

        if (routeImageFileOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        botMessageHelper.sendRoute(chatId, routeImageFileOptional.get(), route);
        cacheManager.remove(String.valueOf(chatId));
    }

    public void deleteRoute(CallbackQuery callbackQuery) {
        var message = callbackQuery.getMessage();
        long routeId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Travel travel = routeService.deleteRouteAndGetTravel(routeId);

        List<Route> travelRoutes = routeService.getTravelRoutes(travel.getId());
        botMessageHelper.sendTravelRoutesMessage(message.getChatId(), message.getMessageId(), travel, travelRoutes);
    }
}
