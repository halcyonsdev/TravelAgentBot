package com.halcyon.travelagent.controller.ticket;

import com.halcyon.travelagent.bot.BotMessageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import static com.halcyon.travelagent.util.KeyboardUtils.generateChoiceOfTicketsKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class TicketController {
    private final BotMessageHelper botMessageHelper;

    public void sendTicketsMenuMessage(CallbackQuery callbackQuery) {
        var ticketsMenuMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text("*Выберите билеты на что вы хотите:*")
                .replyMarkup(generateChoiceOfTicketsKeyboardMarkup())
                .build();
        ticketsMenuMessage.enableMarkdown(true);

        botMessageHelper.editMessage(ticketsMenuMessage);
    }
}
