package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.TravelAgentBot;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static com.halcyon.travelagent.util.KeyboardUtils.generateTravelsInlineKeyboard;

@Controller
@RequiredArgsConstructor
public class TravelController {
    private final TravelService travelService;

    public void handleGettingTravels(TravelAgentBot bot, Update update) {
        List<Travel> userTravels = travelService.getUserTravels(update.getCallbackQuery().getFrom().getId());

        var newMessage = EditMessageText.builder()
                .chatId(update.getCallbackQuery().getMessage().getChatId())
                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                .text("***Мои путешествия***")
                .replyMarkup(generateTravelsInlineKeyboard(userTravels))
                .build();
        newMessage.enableMarkdown(true);

        bot.editMessage(newMessage);
    }
}
