package com.halcyon.travelagent.service;

import com.halcyon.travelagent.entity.Note;
import com.halcyon.travelagent.entity.enums.NoteType;
import com.halcyon.travelagent.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final TravelService travelService;

    public Note createTextNote(long travelId, String name, String text) {
        Note note = Note.builder()
                .name(name)
                .text(text)
                .travel(travelService.findById(travelId))
                .type(NoteType.TEXT)
                .build();

        return noteRepository.save(note);
    }

    public Note createNote(long travelId, String name, String fileId, NoteType type) {
        Note note = Note.builder()
                .name(name)
                .fileId(fileId)
                .travel(travelService.findById(travelId))
                .type(type)
                .build();

        return noteRepository.save(note);
    }

    public Optional<Note> findById(long noteId) {
        return noteRepository.findById(noteId);
    }

    public List<Note> getTravelNotes(long travelId) {
        return noteRepository.findAllByTravelIdOrderByCreatedAt(travelId);
    }

    public int getTravelNotesCount(long travelId) {
        return noteRepository.countNotesByTravelId(travelId);
    }
}
