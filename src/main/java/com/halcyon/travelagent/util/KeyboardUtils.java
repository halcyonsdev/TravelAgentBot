package com.halcyon.travelagent.util;

import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.entity.Travel;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtils {
    private static final int NUMBER_OF_BUTTONS_IN_ROW = 3;

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

            if (currentRow.size() == NUMBER_OF_BUTTONS_IN_ROW) {
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
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (int i = 0; i < locations.size(); i++) {
            currentRow.add(
                    InlineKeyboardButton.builder()
                            .text(String.valueOf(i + 1))
                            .callbackData("choose_location_" + locationIds.get(i))
                            .build()
            );

            if (currentRow.size() == NUMBER_OF_BUTTONS_IN_ROW) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        return new InlineKeyboardMarkup(keyboard);
    }

    public static InlineKeyboardMarkup generateLocationInfoKeyboardMarkup(long locationId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("✍️ Изменить название")
                                .callbackData("change_location_name_" + locationId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83C\uDF06 Изменить улицу")
                                .callbackData("change_location_street_" + locationId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDD53 Изменить время отправления")
                                .callbackData("change_location_start_" + locationId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDD57 Изменить время прибытия")
                                .callbackData("change_location_end_" + locationId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDDD1 Удалить")
                                .callbackData("delete_location_" + locationId)
                                .build()),
                        new InlineKeyboardRow(getBackButton())
                )).build();
    }

    public static InlineKeyboardMarkup generateTravelLocationsKeyboardMarkup(List<Location> locations) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (int i = 0; i < locations.size(); i++) {
            currentRow.add(
                    InlineKeyboardButton.builder()
                            .text(String.valueOf(i + 1))
                            .callbackData("get_location_" + locations.get(i).getId())
                            .build()
            );

            if (currentRow.size() == NUMBER_OF_BUTTONS_IN_ROW) {
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
}
