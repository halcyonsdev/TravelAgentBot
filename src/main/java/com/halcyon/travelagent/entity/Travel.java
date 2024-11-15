package com.halcyon.travelagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "travels")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Travel {
    public Travel(String name, String description, Long creatorId) {
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "creator_id")
    private Long creatorId;
}
