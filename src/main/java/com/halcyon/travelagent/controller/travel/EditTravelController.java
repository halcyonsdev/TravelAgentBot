package com.halcyon.travelagent.controller.travel;

import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.generateChangeTravelKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class EditTravelController {
    private final TravelService travelService;
    private final CacheManager cacheManager;
    private final BotMessageHelper botMessageHelper;

    public void changeTravel(CallbackQuery callbackQuery) {
        String[] splitData = callbackQuery.getData().split("_");
        long travelId = Long.parseLong(splitData[2]);

        Travel travel = travelService.findById(travelId);

        var changeTravelMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(botMessageHelper.getTravelInfoText(travel))
                .replyMarkup(generateChangeTravelKeyboardMarkup(travelId))
                .build();
        changeTravelMessage.enableMarkdown(true);

        botMessageHelper.editMessage(changeTravelMessage);
    }

    public void deleteTravel(CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        List<Travel> userTravels = travelService.deleteTravelAndGetRemainingOnes(travelId);

        botMessageHelper.editMessageToUserTravels(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), userTravels);
    }

    public void enterNewTravelName(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendEnterDataMessage(callbackQuery, "название");

        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(ChatStatusType.CHANGE_TRAVEL_NAME)
                        .data(List.of(String.valueOf(travelId), String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void changeTravelName(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        String newTravelName = message.getText();

        long travelId = Long.parseLong(cachedData.get(0));
        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        if (newTravelName.length() > 100) {
            botMessageHelper.sendInvalidDataMessage(
                    message,
                    "*Длина названия путешествия не дожна превышать 100 символов!* Пожалуйта, введите название снова"
            );
        } else {
            travelService.changeName(travelId, newTravelName);
            cacheManager.remove(String.valueOf(chatId));

            List<Travel> userTravels = travelService.getUserTravels(message.getFrom().getId());
            botMessageHelper.sendUserTravelsMessage(chatId, userTravels);
        }
    }

    public void enterNewTravelDescription(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        travelService.changeDescription(travelId, "отсутствует");

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message message = botMessageHelper.sendEnterDataMessage(callbackQuery, "описание");

        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(ChatStatusType.TRAVEL_DESCRIPTION)
                        .data(List.of(String.valueOf(travelId), String.valueOf(message.getMessageId())))
                        .build()
        );
    }

    public void changeTravelDescription(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        String newTravelDescription = message.getText();

        long travelId = Long.parseLong(cachedData.get(0));
        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        if (newTravelDescription.length() > 500) {
            botMessageHelper.sendInvalidDataMessage(
                    message,
                    "*Длина описания путешествия не должна превышать 500 символов!* Пожалуйта, введите описание снова"
            );
        }

        travelService.changeDescription(travelId, newTravelDescription);
        cacheManager.remove(String.valueOf(chatId));

        List<Travel> userTravels = travelService.getUserTravels(message.getFrom().getId());
        botMessageHelper.sendUserTravelsMessage(chatId, userTravels);
    }
}
