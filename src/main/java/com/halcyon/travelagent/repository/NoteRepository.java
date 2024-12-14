package com.halcyon.travelagent.repository;

import com.halcyon.travelagent.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByTravelIdOrderByCreatedAt(long travelId);

    @Query("SELECT COUNT(*) FROM Note n WHERE n.travel.id = :travelId")
    int countNotesByTravelId(long travelId);
}
