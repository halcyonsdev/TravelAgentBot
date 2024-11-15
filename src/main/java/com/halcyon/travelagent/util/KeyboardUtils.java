package com.halcyon.travelagent.util;

import com.halcyon.travelagent.entity.Travel;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtils {
    private KeyboardUtils() {}

    public static InlineKeyboardMarkup generateStartInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("✈️ Мои путешествия")
                                .callbackData("get_travels")
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDD28 Создать путешествие")
                                .callbackData("create_travel")
                                .build())
                        )).build();
    }

    public static InlineKeyboardMarkup generateTravelsInlineKeyboard(List<Travel> travels) {
        int numberOfButtonsInRow = 3;

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();

        int counter = 1;
        for (Travel travel: travels) {
            var travelButton = InlineKeyboardButton.builder()
                    .text(travel.getName().isEmpty() ? "Новое путешествие " + counter++ : travel.getName())
                    .callbackData("info_travel_" + travel.getId())
                    .build();

            row.add(travelButton);

            if (row.size() == numberOfButtonsInRow) {
                keyboard.add(row);
                row = new InlineKeyboardRow();
            }
        }

        if (!row.isEmpty()) {
            row.add(getBackButton());
            keyboard.add(row);
        } else {
            keyboard.add(new InlineKeyboardRow(getBackButton()));
        }

        return new InlineKeyboardMarkup(keyboard);
    }

    public static InlineKeyboardButton getBackButton() {
        return InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("back")
                .build();
    }
}
