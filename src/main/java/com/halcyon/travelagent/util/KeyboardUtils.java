package com.halcyon.travelagent.util;

import com.halcyon.travelagent.entity.*;
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

        for (Travel travel : travels) {
            var travelButton = InlineKeyboardButton.builder()
                    .callbackData("info_travel_" + travel.getId())
                    .text(travel.getName())
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

    public static InlineKeyboardButton getBackWithDeleteButton(int messageId) {
        return InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("back_" + messageId)
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
                                .text("\uD83D\uDDFA Маршруты")
                                .callbackData("routes_travel_" + travelId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDDD2 Заметки")
                                .callbackData("travel_notes_" + travelId)
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
                                .text("\uD83D\uDE97 Построить маршрут")
                                .callbackData("build_route_" + travelId)
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

    public static InlineKeyboardMarkup generateGetTravelLocationsKeyboardMarkup(List<Location> locations, long travelId) {
        return generateTravelLocationsKeyboardMarkup(locations, "get_location_", travelId);
    }

    private static InlineKeyboardMarkup generateTravelLocationsKeyboardMarkup(List<Location> locations, String callbackData, long travelId) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (int i = 0; i < locations.size(); i++) {
            currentRow.add(
                    InlineKeyboardButton.builder()
                            .text(String.valueOf(i + 1))
                            .callbackData(callbackData + locations.get(i).getId())
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

        if (travelId != -1) {
            var createLocationButton = InlineKeyboardButton.builder()
                    .text("\uD83D\uDCE5 Добавить локацию")
                    .callbackData("add_travel_location_" + travelId)
                    .build();

            keyboard.add(new InlineKeyboardRow(createLocationButton));
            keyboard.add(new InlineKeyboardRow(getBackButton()));
        }

        return new InlineKeyboardMarkup(keyboard);
    }

    public static InlineKeyboardMarkup generateStartPointChoiceKeyboardMarkup(List<Location> locations) {
        return generateTravelLocationsKeyboardMarkup(locations, "route_start_location_", -1);
    }

    public static InlineKeyboardMarkup generateDestinationPointChoiceKeyboardMarkup(List<Location> locations) {
        return generateTravelLocationsKeyboardMarkup(locations, "route_destination_location_", -1);
    }

    public static InlineKeyboardMarkup generateTravelRoutesInlineKeyboard(List<Route> routes, long travelId) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (Route route : routes) {
            var routeButton = InlineKeyboardButton.builder()
                    .callbackData("info_route_" + route.getId())
                    .text(route.getName())
                    .build();

            currentRow.add(routeButton);

            if (currentRow.size() == NUMBER_OF_BUTTONS_IN_ROW) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        var createRouteButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDE97 Построить маршрут")
                .callbackData("build_route_" + travelId)
                .build();

        keyboard.add(new InlineKeyboardRow(createRouteButton));
        keyboard.add(new InlineKeyboardRow(getBackButton()));

        return new InlineKeyboardMarkup(keyboard);
    }

    public static InlineKeyboardMarkup generateRouteInfoInlineKeyboard(long routeId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("✍️ Изменить название")
                                .callbackData("change_route_name_" + routeId)
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .callbackData("add_point_route_" + routeId)
                                .text("\uD83D\uDCE5 Добавить точку")
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .callbackData("delete_route_point_" + routeId)
                                .text("❌ Удалить точку")
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .callbackData("delete_route_" + routeId)
                                .text("\uD83D\uDDD1 Удалить")
                                .build()),
                        new InlineKeyboardRow(getBackButton())
                ))
                .build();
    }

    public static InlineKeyboardMarkup generateAddPointLocationsKeyboardMarkup(List<Location> locations) {
        return generateTravelLocationsKeyboardMarkup(locations, "add_point_location_", -1);
    }

    public static InlineKeyboardMarkup generateChoosePointForAddingKeyboardMarkup(List<RoutePoint> routePoints) {
        return generateChooseRoutePointKeyboardMarkup(routePoints, "choose_add_point_");
    }

    public static InlineKeyboardMarkup generateChoosePointForDeletingKeyboardMarkup(List<RoutePoint> routePoints) {
        return generateChooseRoutePointKeyboardMarkup(routePoints, "choose_delete_point_");
    }

    private static InlineKeyboardMarkup generateChooseRoutePointKeyboardMarkup(List<RoutePoint> routePoints, String callbackData) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        if (callbackData.equals("choose_add_point_")) {
            var newStartPointButton = InlineKeyboardButton.builder()
                    .callbackData("new_start_point")
                    .text("В начало")
                    .build();
            currentRow.add(newStartPointButton);
        }

        for (int i = 0; i < routePoints.size(); i++) {
            currentRow.add(
                    InlineKeyboardButton.builder()
                            .callbackData(callbackData + routePoints.get(i).getId())
                            .text(String.valueOf(i + 1))
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

    public static InlineKeyboardMarkup generateTravelNotesKeyboardMarkup(List<Note> notes, long travelId) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (Note note : notes) {
            var noteButton = InlineKeyboardButton.builder()
                    .callbackData("note_info_" + note.getId())
                    .text(note.getName())
                    .build();

            currentRow.add(noteButton);

            if (currentRow.size() == NUMBER_OF_BUTTONS_IN_ROW) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        var createNoteButton = InlineKeyboardButton.builder()
                .text("✍️ Создать заметку")
                .callbackData("create_travel_note_" + travelId)
                .build();

        keyboard.add(new InlineKeyboardRow(createNoteButton));
        keyboard.add(new InlineKeyboardRow(getBackButton()));

        return new InlineKeyboardMarkup(keyboard);
    }

    public static InlineKeyboardMarkup generateNoteInfoKeyboardMarkup(long noteId, int messageId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("✍️ Поменять название")
                                .callbackData("change_note_name_" + noteId + (messageId != -1 ? "_message_" + messageId : ""))
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDEE0 Поменять содержимое")
                                .callbackData("change_note_content_" + noteId + (messageId != -1 ? "_message_" + messageId : ""))
                                .build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder()
                                .text("\uD83D\uDDD1 Удалить")
                                .callbackData("delete_note_" + noteId + (messageId != -1 ? "_message_" + messageId : ""))
                                .build()),
                        new InlineKeyboardRow(messageId != -1 ? getBackWithDeleteButton(messageId) : getBackButton())
                ))
                .build();
    }
}
