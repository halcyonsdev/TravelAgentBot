package com.halcyon.travelagent.controller;

import com.halcyon.travelagent.bot.BotMessageHelper;
import com.halcyon.travelagent.caching.CacheManager;
import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Note;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.entity.enums.NoteType;
import com.halcyon.travelagent.service.NoteService;
import com.halcyon.travelagent.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.util.KeyboardUtils.generateTravelNotesKeyboardMarkup;

@Controller
@RequiredArgsConstructor
public class CreateNoteController {
    private final CacheManager cacheManager;
    private final BotMessageHelper botMessageHelper;
    private final TravelService travelService;
    private final NoteService noteService;

    public void enterNoteName(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[3]);

        if (noteService.getTravelNotesCount(travelId) >= 20) {
            sendExceededLimitMessage(chatId);
            return;
        }

        var enterNoteMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, введите название для заметки")
                .build();

        botMessageHelper.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
        Message sentMessage = botMessageHelper.sendMessage(enterNoteMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.NOTE_NAME)
                        .data(List.of(String.valueOf(travelId), String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    private void sendExceededLimitMessage(long chatId) {
        var exceededLimitMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Вы не можете создать больше 20 заметок для одного путешествии")
                .build();

        botMessageHelper.sendMessage(exceededLimitMessage);
    }

    public void enterNoteContent(Message message, String travelId, int messageId) {
        long chatId = message.getChatId();

        botMessageHelper.deleteMessage(chatId, messageId);

        if (message.getText().length() > 50) {
            String errorText = "*Длина названия заметки не дожна превышать 50 символов!* Пожалуйта, введите название снова";
            botMessageHelper.sendInvalidDataMessage(message, errorText);
            return;
        }

        var enterContentMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста, отправьте текст / аудио / фото / файл для заметки")
                .build();

        botMessageHelper.deleteMessage(chatId, message.getMessageId());

        Message sentMessage = botMessageHelper.sendMessage(enterContentMessage);

        cacheManager.saveChatStatus(
                chatId,
                ChatStatus.builder()
                        .type(ChatStatusType.NOTE_CONTENT)
                        .data(List.of(travelId, message.getText(), String.valueOf(sentMessage.getMessageId())))
                        .build()
        );
    }

    public void createNote(Message message, List<String> cachedData) {
        long chatId = message.getChatId();
        int messageId = Integer.parseInt(cachedData.get(2));

        botMessageHelper.deleteMessage(chatId, messageId);

        if (message.hasText() && message.getText().length() > 500) {
            String errorText = "*Длина текста заметки не дожна превышать 500 символов!* Пожалуйта, отправьте текст / аудио / фото / файл снова";
            botMessageHelper.sendInvalidDataMessage(message, errorText);
            return;
        }

        long travelId = Long.parseLong(cachedData.get(0));
        String noteName = cachedData.get(1);

        Note note;
        if (message.hasText()) {
            note = noteService.createTextNote(travelId, noteName, message.getText());
        } else if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            note = noteService.createNote(travelId, noteName, photos.get(photos.size() - 1).getFileId(), NoteType.PHOTO);
        } else if (message.hasDocument()) {
            note = noteService.createNote(travelId, noteName, message.getDocument().getFileId(), NoteType.FILE);
        } else if (message.hasVoice()) {
            note = noteService.createNote(travelId, noteName, message.getVoice().getFileId(), NoteType.VOICE);
        } else {
            String errorText = "Пожалуйта, отправьте текст / аудио / фото / файл!";
            botMessageHelper.sendInvalidDataMessage(message, errorText);
            return;
        }

        botMessageHelper.deleteMessage(chatId, message.getMessageId());
        botMessageHelper.sendNoteInfo(chatId, message.getMessageId(), note);
        cacheManager.remove(String.valueOf(chatId));
    }

    public void sendTravelNotes(CallbackQuery callbackQuery) {
        long travelId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Travel travel = travelService.findById(travelId);
        List<Note> travelNotes = noteService.getTravelNotes(travelId);

        var travelNotesMessage = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(String.format("*Заметки в путешествии \"%s\"*", travel.getName()))
                .replyMarkup(generateTravelNotesKeyboardMarkup(travelNotes, travelId))
                .build();
        travelNotesMessage.enableMarkdown(true);

        botMessageHelper.editMessage(travelNotesMessage);
    }

    public void sendNoteInfo(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        long noteId = Long.parseLong(callbackQuery.getData().split("_")[2]);
        Optional<Note> noteOptional = noteService.findById(noteId);

        if (noteOptional.isEmpty()) {
            botMessageHelper.sendErrorMessage(chatId);
        } else {
            botMessageHelper.sendNoteInfo(chatId, callbackQuery.getMessage().getMessageId(), noteOptional.get());
        }
    }
}
