package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Note;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.entity.enums.NoteType;
import com.halcyon.travelagent.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class EditNoteController {
    private final CacheManager cacheManager;
    private final BotMessageHelper botMessageHelper;
    private final NoteService noteService;

    public void enterNoteNewName(CallbackQuery callbackQuery, boolean isToDelete) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] callbackData = callbackQuery.getData().split("_");
        String noteId = callbackData[3];

        var enterNewNameMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите новое название для заметки")
                .build();

        if (isToDelete) {
            int messageId = Integer.parseInt(callbackData[5]);
            botMessageHelper.deleteMessage(chatId, messageId);
        }

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendMessage(enterNewNameMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.CHANGE_NOTE_NAME)
                        .data(List.of(noteId, String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void changeNoteName(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        long noteId = Long.parseLong(cachedData.get(0));
        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));

        if (message.getText().length() > 50) {
            String errorText = "*Длина названия заметки не дожна превышать 50 символов!* Пожалуйта, введите название снова";
            botMessageHelper.sendInvalidDataMessage(message, errorText);
            return;
        }

        Optional<Note> noteOptional = noteService.changeName(noteId, message.getText());

        if (noteOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        botMessageHelper.sendNoteInfo(chatId, message.getMessageId(), noteOptional.get());
        cacheManager.remove(String.valueOf(chatId));
    }

    public void deleteNote(CallbackQuery callbackQuery, boolean isToDelete) {
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        long noteId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Optional<Travel> travelOptional = noteService.deleteNoteAndGetTravel(noteId);

        if (travelOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        Travel travel = travelOptional.get();
        List<Note> travelNotes = noteService.getTravelNotes(travel.getId());

        if (isToDelete) {
            int toDeleteMessageId = Integer.parseInt(callbackQuery.getData().split("_")[4]);
            botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        }

        botMessageHelper.sendTravelNotesMessage(chatId, messageId, travel, travelNotes);
    }

    public void enterNoteNewContent(CallbackQuery callbackQuery, boolean isToDelete) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] callbackData = callbackQuery.getData().split("_");
        String noteId = callbackData[3];

        var enterNewContentMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, отправьте новый(-ое) текст / аудио / фото / файл для заметки")
                .build();

        if (isToDelete) {
            int toDeleteMessageId = Integer.parseInt(callbackData[5]);
            botMessageHelper.deleteMessage(chatId, toDeleteMessageId);
        }

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendMessage(enterNewContentMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.CHANGE_NOTE_CONTENT)
                        .data(List.of(noteId, String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void changeNoteContent(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        long noteId = Long.parseLong(cachedData.get(0));
        int toDeleteMessageId = Integer.parseInt(cachedData.get(1));

        botMessageHelper.deleteMessage(chatId, toDeleteMessageId);

        if (message.hasText() && message.getText().length() > 500) {
            botMessageHelper.deleteMessage(chatId, message.getMessageId());
            String errorText = "*Длина текста заметки не дожна превышать 500 символов!* Пожалуйта, отправьте новый(-ое) текст / аудио / фото / файл снова";
            botMessageHelper.sendInvalidDataMessage(message, errorText);
            return;
        }

        Optional<Note> updatedNoteOptional;
        if (message.hasText()) {
            updatedNoteOptional = noteService.changeNoteTypeToText(noteId, message.getText());
        } else if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            updatedNoteOptional = noteService.changeNoteContent(noteId, photos.get(photos.size() - 1).getFileId(), NoteType.PHOTO);
        } else if (message.hasDocument()) {
            updatedNoteOptional = noteService.changeNoteContent(noteId, message.getDocument().getFileId(), NoteType.FILE);
        } else if (message.hasVoice()) {
            updatedNoteOptional = noteService.changeNoteContent(noteId, message.getVoice().getFileId(), NoteType.VOICE);
        } else {
            String errorText = "Пожалуйта, отправьте новый(-ое) текст / аудио / фото / файл!";
            botMessageHelper.sendInvalidDataMessage(message, errorText);
            return;
        }

        if (updatedNoteOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
            return;
        }

        botMessageHelper.sendNoteInfo(chatId, message.getMessageId(), updatedNoteOptional.get());
        cacheManager.remove(String.valueOf(chatId));
    }
}
