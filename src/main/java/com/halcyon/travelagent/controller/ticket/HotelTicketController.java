package com.halcyon.travelagent.controller.ticket;

import com.halcyon.travelagent.api.hotellook.HotelLookAPI;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.generateHotelsInlineKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class HotelTicketController {
    private final BotMessageHelper botMessageHelper;
    private final CacheManager cacheManager;
    private final HotelLookAPI hotelLookAPI;

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    public void enterHotelCity(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        var enterHotelCityMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите название города, в котором вам нужен отель")
                .build();

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendMessage(enterHotelCityMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.HOTEL_CITY)
                        .data(List.of(String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void enterCheckInDate(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        int toDeleteMessageId = Integer.parseInt(cachedData.get(0));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        Message sentMessage = sendEnterHotelTimeMessage(chatId, true);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.HOTEL_CHECK_IN)
                        .data(List.of(message.getText(), String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    private Message sendEnterHotelTimeMessage(long chatId, boolean isCheckIn) {
        String text = String.format(
                "Пожалуйста, введите время %s (в формате %s)",
                (isCheckIn ? "заезда в отель" : "выезда из отеля"),
                DATE_PATTERN
        );

        var enterHotelTimeMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        return botMessageHelper.sendMessage(enterHotelTimeMessage);
    }

    public void enterCheckOutDate(Message message, List<String> cachedData) {
        long chatId = message.getChatId();

        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            LocalDate.parse(message.getText(), dateTimeFormatter);

            Message sentMessage = sendEnterHotelTimeMessage(chatId, false);

            cacheManager.saveChatStatus(
                    chatId,
                    ChatStatus.builder()
                            .type(ChatStatusType.HOTEL_CHECK_OUT)
                            .data(List.of(cachedData.get(0), message.getText(), String.valueOf(sentMessage.getMessageId())))
                            .build()
            );
        } catch (DateTimeParseException e) {
            sendErrorHotelTimeMessage(chatId, true);
        }
    }

    private void sendErrorHotelTimeMessage(long chatId, boolean isCheckIn) {
        String text = String.format(
                "Введенные данные не соответствуют формату %s. Пожалуйста, введите время %s по указанному формату.",
                DATE_PATTERN,
                (isCheckIn ? "заезда в отель" : "выезда из отеля")
        );

        var errorHotelTimeMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        botMessageHelper.sendMessage(errorHotelTimeMessage);
    }

    public void findHotels(Message message, List<String> cachedData) {
        long chatId = message.getChatId();


        int toDeleteMessageId = Integer.parseInt(cachedData.get(2));
        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            LocalDate.parse(message.getText(), dateTimeFormatter);

            String city = cachedData.get(0);
            String checkIn = cachedData.get(1);
            String checkOut = message.getText();

            var hotelsInfoOptional = hotelLookAPI.getCityHotelsInfo(city, checkIn, checkOut, 0);

            if (hotelsInfoOptional.isEmpty()) {
                botMessageHelper.sendErrorMessage(chatId);
                return;
            }

            var hotelsInfoMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(hotelsInfoOptional.get())
                    .replyMarkup(generateHotelsInlineKeyboardMarkup(city, checkIn, checkOut, 0))
                    .build();
            hotelsInfoMessage.enableMarkdown(true);

            botMessageHelper.sendMessage(hotelsInfoMessage);
            cacheManager.remove(String.valueOf(chatId));

        } catch (DateTimeParseException e) {
            sendErrorHotelTimeMessage(chatId, false);
        }
    }

    public void goInHotelOrder(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        String[] callbackData = callbackQuery.getData().split("_");
        String city = callbackData[3];
        String checkIn = callbackData[4];
        String checkOut = callbackData[5];
        int order = Integer.parseInt(callbackData[6]);

        var hotelsInfoOptional = hotelLookAPI.getCityHotelsInfo(city, checkIn, checkOut, order);

        if (hotelsInfoOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        var hotelsInfoMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(hotelsInfoOptional.get())
                .replyMarkup(generateHotelsInlineKeyboardMarkup(city, checkIn, checkOut, order))
                .build();
        hotelsInfoMessage.enableMarkdown(true);

        botMessageHelper.editMessage(hotelsInfoMessage);
    }
}
