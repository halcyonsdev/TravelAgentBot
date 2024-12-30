package com.halcyon.travelagent.controller.sight;

import com.halcyon.travelagent.api.sightsafari.SightInfo;
import com.halcyon.travelagent.api.sightsafari.SightSafariAPI;
import com.halcyon.travelagent.api.yandex.CityArea;
import com.halcyon.travelagent.api.yandex.YandexAPI;
import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.generateSightsInfoKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class SightController {
    private final BotMessageHelper botMessageHelper;
    private final CacheManager cacheManager;
    private final YandexAPI yandexAPI;
    private final SightSafariAPI sightSafariAPI;

    public void enterSightCity(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

        var enterCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите название города, в котором хотите посмотреть достопримечательности")
                .build();

        Message sentMessage = botMessageHelper.sendMessage(enterCityMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.SIGHT_CITY)
                        .data(List.of(String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void getSights(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        String city = message.getText();

        int toDeleteMessageId = Integer.parseInt(cachedData.get(0));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        sendSightsMessage(city, 0, chatId);
        cacheManager.remove(String.valueOf(chatId));
    }

    private void sendSightsMessage(String city, int order, long chatId) {
        Optional<List<SightInfo>> sightsInfoOptional = getSightsInfo(city, order, chatId);

        if (sightsInfoOptional.isEmpty()) {
            return;
        }

        List<SightInfo> sights = sightsInfoOptional.get();

        var sightsMessage = SendMessage.builder()
                .chatId(chatId)
                .text(getSightsInfoText(sights, city, order))
                .replyMarkup(generateSightsInfoKeyboardMarkup(sights, city, order))
                .build();
        sightsMessage.enableMarkdown(true);
        sightsMessage.disableWebPagePreview();

        botMessageHelper.sendMessage(sightsMessage);
    }

    private Optional<List<SightInfo>> getSightsInfo(String city, int order, long chatId) {
        Optional<CityArea> cityAreaOptional = yandexAPI.getCityArea(city);

        if (cityAreaOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return Optional.empty();
        }

        Optional<List<SightInfo>> sightsInfoOptional = sightSafariAPI.getCitySightsInfo(cityAreaOptional.get(), order);

        if (sightsInfoOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return Optional.empty();
        }

        return sightsInfoOptional;
    }

    private String getSightsInfoText(List<SightInfo> sights, String city, int order) {
        StringBuilder sightInfoText = new StringBuilder(String.format("*Достопримечательности в городе \"%s\"*%n%n", city));

        for (SightInfo sightInfo : sights) {
            sightInfoText.append(String.format("""
                    %s. *%s* %s
                    %s
                    """,
                    ++order, sightInfo.getName(), sightInfo.getType(), sightInfo.getLinks()
            ));
        }

        return sightInfoText.toString();
    }

    public void goInSightOrder(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] callbackData = callbackQuery.getData().split("_");

        String city = callbackData[3];
        int order = Integer.parseInt(callbackData[4]);

        Optional<List<SightInfo>> sightsInfoOptional = getSightsInfo(city, order, chatId);

        if (sightsInfoOptional.isEmpty()) {
            return;
        }

        List<SightInfo> sights = sightsInfoOptional.get();
        var sightsMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getSightsInfoText(sights, city, order))
                .replyMarkup(generateSightsInfoKeyboardMarkup(sights, city, order))
                .build();
        sightsMessage.enableMarkdown(true);
        sightsMessage.disableWebPagePreview();

        botMessageHelper.editMessage(sightsMessage);
    }

    public void sendSightLocation(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] callbackData = callbackQuery.getData().split("_");

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

        String city = callbackData[2];
        int order = Integer.parseInt(callbackData[3]);
        double latitude = Double.parseDouble(callbackData[4]);
        double longitude = Double.parseDouble(callbackData[5]);

        var sightLocation = SendLocation.builder()
                .chatId(chatId)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        botMessageHelper.sendLocation(sightLocation);


        sendSightsMessage(city, order, chatId);
    }
}
