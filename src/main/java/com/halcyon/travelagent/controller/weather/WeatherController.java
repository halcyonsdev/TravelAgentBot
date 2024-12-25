package com.halcyon.travelagent.controller.weather;

import com.halcyon.travelagent.api.openweathermap.OpenWeatherMapAPI;
import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.generateWeatherInfoKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class WeatherController {
    private final BotMessageHelper botMessageHelper;
    private final CacheManager cacheManager;
    private final OpenWeatherMapAPI openWeatherMapAPI;

    public void enterWeatherCity(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        var enterWeatherMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите название города для прогноза погоды")
                .build();

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendMessage(enterWeatherMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.WEATHER_CITY)
                        .data(List.of(String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void sendCityWeatherInfo(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        Optional<String> forecastsOptional = openWeatherMapAPI.getCityWeatherInfo(message.getText(), 0);

        if (forecastsOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        int toDeleteMessageId = Integer.parseInt(cachedData.get(0));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        var forecastsMessage = SendMessage.builder()
                .chatId(chatId)
                .text(forecastsOptional.get())
                .replyMarkup(generateWeatherInfoKeyboardMarkup(message.getText(), 0))
                .build();
        forecastsMessage.enableMarkdown(true);

        botMessageHelper.sendMessage(forecastsMessage);
        cacheManager.remove(String.valueOf(chatId));
    }

    public void goInWeatherOrder(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] callbackData = callbackQuery.getData().split("_");
        String city = callbackData[3];
        int order = Integer.parseInt(callbackData[4]);

        Optional<String> forecastsOptional = openWeatherMapAPI.getCityWeatherInfo(city, order);

        if (forecastsOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        var forecastsMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(forecastsOptional.get())
                .replyMarkup(generateWeatherInfoKeyboardMarkup(city, order))
                .build();
        forecastsMessage.enableMarkdown(true);

        botMessageHelper.editMessage(forecastsMessage);
    }
}
