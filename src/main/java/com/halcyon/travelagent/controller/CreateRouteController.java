package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.api.geoapify.GeoapifyAPI;
import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.entity.Route;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.LocationService;
import com.halcyon.travelagent.service.RouteService;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.*;

@Controller
@RequiredArgsConstructor
public class CreateRouteController {
    private final LocationService locationService;
    private final CacheManager cacheManager;
    private final BotMessageHelper botMessageHelper;
    private final RouteService routeService;
    private final TravelService travelService;
    private final GeoapifyAPI geoapifyAPI;

    public void chooseStartPoint(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);

        if (routeService.getTravelRoutesCount(travelId) >= 20) {
            sendExceededLimitMessage(chatId);
            return;
        }

        List<Location> locations = locationService.getTravelLocations(travelId);

        if (locations.isEmpty() || locations.size() == 1) {
            var errorMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text("*У вас нет созданных локаций или их недостаточно*. Для построения маршрута нужно больше 1 локации")
                    .build();
            errorMessage.enableMarkdown(true);

            botMessageHelper.sendMessage(errorMessage);
        } else {
            String text = botMessageHelper.getTravelLocationsText(locations);

            var startPointsMessage = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("*Выберите начальную точку маршрута:*\n\n" + text)
                    .replyMarkup(generateStartPointChoiceKeyboardMarkup(locations))
                    .build();
            startPointsMessage.enableMarkdown(true);

            botMessageHelper.editMessage(startPointsMessage);

            cacheManager.saveChatStatus(
                    chatId,
                    ChatStatus.builder()
                            .type(ChatStatusType.ROUTE_START_POINT)
                            .data(List.of(String.valueOf(travelId)))
                            .build()
            );
        }
    }

    private void sendExceededLimitMessage(long chatId) {
        var exceededLimitMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Вы не можете создать больше 20 маршрутов в одном путешествии")
                .build();

        botMessageHelper.sendMessage(exceededLimitMessage);
    }

    public void chooseDestinationPoint(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String startLocationId = callbackQuery.getData().split("_")[3];

        ChatStatus chatStatus = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class).get();
        long travelId = Long.parseLong(chatStatus.getData().get(0));

        List<Location> locations = locationService.getTravelLocations(travelId);
        Location startLocation = locationService.findById(Long.parseLong(startLocationId));
        locations.remove(startLocation);

        String text = botMessageHelper.getTravelLocationsText(locations);

        var destinationLocationsMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text("*Выберите конечную точку маршрута:*\n\n" + text)
                .replyMarkup(generateDestinationPointChoiceKeyboardMarkup(locations))
                .build();
        destinationLocationsMessage.enableMarkdown(true);

        botMessageHelper.editMessage(destinationLocationsMessage);

        String startPointName = startLocation.getName();
        if (!startLocation.getStreet().equals("отсутствует")) {
            startPointName += ", улица " + startLocation.getStreet();
        }

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.ROUTE_DESTINATION_POINT)
                        .data(List.of(String.valueOf(travelId), startPointName))
                        .build()
        );
    }

    public void enterRouteName(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String destinationLocationId = callbackQuery.getData().split("_")[3];

        Location destinationLocation = locationService.findById(Long.parseLong(destinationLocationId));

        List<String> cacheData = cacheManager.fetch(String.valueOf(chatId), ChatStatus.class).get().getData();

        var enterRouteNameMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Теперь введите название для маршрута")
                .build();

        botMessageHelper.sendMessage(enterRouteNameMessage);

        String destinationPointName = destinationLocation.getName();
        if (!destinationLocation.getStreet().equals("отсутствует")) {
            destinationPointName += ", улица " + destinationLocation.getStreet();
        }

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.ROUTE_NAME)
                        .data(List.of(cacheData.get(0), cacheData.get(1), destinationPointName))
                        .build()
        );
    }

    public void createRoute(Message message, List<String> cacheData) {
        long chatId = message.getChatId();
        long travelId = Long.parseLong(cacheData.get(0));
        String startPointName = cacheData.get(1);
        String destinationPointName = cacheData.get(2);

        if (message.getText().length() > 100) {
            botMessageHelper.sendInvalidRouteNameMessage(chatId);
            return;
        }

        try {
            Optional<String> routeImageUrlOptional = geoapifyAPI.getNewRouteImageUrl(startPointName, destinationPointName);

            if (routeImageUrlOptional.isEmpty()) {
                cacheManager.remove(String.valueOf(chatId));
                botMessageHelper.sendErrorMessage(chatId);
                return;
            }

            Optional<InputFile> routeImageOptional = geoapifyAPI.getRouteImageFile(routeImageUrlOptional.get());

            if (routeImageOptional.isEmpty()) {
                cacheManager.remove(String.valueOf(chatId));
                botMessageHelper.sendErrorMessage(chatId);
                return;
            }

            InputFile routeImage = routeImageOptional.get();
            Travel travel = travelService.findById(travelId);
            Route route = routeService.createRoute(message.getText(), startPointName, destinationPointName, routeImageUrlOptional.get(), travel);

            botMessageHelper.sendRoute(chatId, routeImage, route);
        } catch (Exception e) {
            botMessageHelper.sendErrorMessage(chatId);
        }
    }

    public void getTravelRoutes(CallbackQuery callbackQuery) {
        var message = callbackQuery.getMessage();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Travel travel = travelService.findById(travelId);

        List<Route> travelRoutes = routeService.getTravelRoutes(travel.getId());
        botMessageHelper.sendTravelRoutesMessage(message.getChatId(), message.getMessageId(), travel, travelRoutes);
    }

    public void getRoute(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long routeId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Route route = routeService.findById(routeId);

        Optional<InputFile> routeImageOptional = geoapifyAPI.getRouteImageFile(route.getApiMapUrl());

        if (routeImageOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        botMessageHelper.sendRoute(chatId, routeImageOptional.get(), route);
    }
}
