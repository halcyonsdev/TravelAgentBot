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
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        int counter = 1;
        for (Travel travel: travels) {
            boolean isEmptyTravelName = travel.getName().isEmpty();
            String data = "info_travel_" + travel.getId();

            var travelButton = InlineKeyboardButton.builder()
                    .callbackData(isEmptyTravelName ? data + "_number_" + counter : data)
                    .text(isEmptyTravelName ? "Новое путешествие " + counter++ : travel.getName())
                    .build();

            currentRow.add(travelButton);

            if (currentRow.size() == numberOfButtonsInRow) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            currentRow.add(getBackButton());
            keyboard.add(currentRow);
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

    public static InlineKeyboardMarkup generateTravelInfoKeyboardMarkup(long travelId, int number) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDCDD Изменить")
                                .callbackData(String.format("change_travel_%s%s", travelId, (number == -1 ? "" : "_number_" + number)))
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83C\uDFD9 Локации")
                                .callbackData("locations_travel_" + travelId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("➕ Добавить локацию")
                                .callbackData("add_travel_location_" + travelId)
                                .build()),
                        new InlineKeyboardRow(getBackButton())
                )).build();
    }

    public static InlineKeyboardMarkup generateChangeTravelKeyboardMarkup(long travelId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("✏️ Изменить название")
                                .callbackData("change_travel_name_" + travelId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDCDD Изменить описание")
                                .callbackData("change_travel_description_" + travelId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDDD1️ Удалить")
                                .callbackData("delete_travel_" + travelId)
                                .build()),
                        new InlineKeyboardRow(getBackButton())
                )).build();
    }

    public static InlineKeyboardMarkup generateChoiceOfLocationsKeyboardMarkup(List<String> locations, List<Long> locationIds) {
        int numberOfButtonsInRow = 3;

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (int i = 0; i < locations.size(); i++) {
            currentRow.add(
                    InlineKeyboardButton.builder()
                            .text(String.valueOf(i + 1))
                            .callbackData("choose_location_" + locationIds.get(i))
                            .build()
            );

            if (currentRow.size() == numberOfButtonsInRow) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        return new InlineKeyboardMarkup(keyboard);
    }

    public static InlineKeyboardMarkup generateLocationInfoKeyboardMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(getBackButton())
                )).build();
    }

    public static InlineKeyboardMarkup generateTravelLocationsKeyboardMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(getBackButton())
                )).build();
    }
}
