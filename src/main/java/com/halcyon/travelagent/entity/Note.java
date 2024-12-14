package com.halcyon.travelagent.entity;

import com.halcyon.travelagent.entity.enums.NoteType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notes")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "name")
    private String name;

    @Column(name = "text")
    private String text;

    @Column(name = "type")
    private NoteType type;

    @Column(name = "file_id")
    private String fileId;

    @ManyToOne
    @JoinColumn(name = "travel_id", referencedColumnName = "id")
    private Travel travel;
}
