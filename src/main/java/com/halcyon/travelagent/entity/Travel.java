package com.halcyon.travelagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "travels")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Travel {
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

    @OneToMany(mappedBy = "travel")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<Location> locations;

    @OneToMany(mappedBy = "travel")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<Route> routes;
}
