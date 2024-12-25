package com.halcyon.travelagent.controller.travel;

import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.*;

@Controller
@RequiredArgsConstructor
public class CreateTravelController {
    private final TravelService travelService;
    private final CacheManager cacheManager;
    private final BotMessageHelper botMessageHelper;

    public void enterTravelName(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();

        if (travelService.getUserTravelsCount(callbackQuery.getFrom().getId()) >= 10) {
            sendExceededLimitMessage(callbackQuery.getFrom().getId());
            return;
        }

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendEnterDataMessage(callbackQuery, "название");

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.TRAVEL_NAME)
                        .data(List.of(String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    private void sendExceededLimitMessage(long chatId) {
        var exceededLimitMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Вы не можете создать больше 10 путешествий")
                .build();

        botMessageHelper.sendMessage(exceededLimitMessage);
    }

    public void editMessageToUserTravels(long userId, long chatId, int messageId) {
        List<Travel> userTravels = travelService.getUserTravels(userId);
        botMessageHelper.editMessageToUserTravels(chatId, messageId, userTravels);
    }

    public void createTravel(Message message, List<String> cachedData) {
        long userId = message.getFrom().getId();
        long chatId = message.getChatId();
        String travelName = message.getText();

        botMessageHelper.deleteMessage(chatId, Integer.parseInt(cachedData.get(0)));
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        if (travelName.length() > 100) {
            botMessageHelper.sendInvalidDataMessage(
                    message,
                    "*Длина названия путешествия не дожна превышать 100 символов!* Пожалуйта, введите название снова"
            );
        } else {
            travelService.createTravel(travelName, userId);
            List<Travel> userTravels = travelService.getUserTravels(userId);

            botMessageHelper.sendUserTravelsMessage(chatId, userTravels);
            cacheManager.remove(String.valueOf(chatId));
        }
    }

    public void getTravel(CallbackQuery callbackQuery) {
        String[] splitData = callbackQuery.getData().split("_");
        long travelId = Long.parseLong(splitData[2]);

        Travel travel = travelService.findById(travelId);

        var travelInfoMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(botMessageHelper.getTravelInfoText(travel))
                .replyMarkup(generateTravelInfoKeyboardMarkup(travelId, splitData.length == 5 ? Integer.parseInt(splitData[4]): -1))
                .build();
        travelInfoMessage.enableMarkdown(true);

        botMessageHelper.editMessage(travelInfoMessage);
    }
}