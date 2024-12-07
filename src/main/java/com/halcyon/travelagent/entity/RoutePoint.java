package com.halcyon.travelagent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_points")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoutePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "route_id", referencedColumnName = "id")
    private Route route;

    @OneToOne
    @JoinColumn(name = "next_point_id", referencedColumnName = "id")
    private RoutePoint nextPoint;
}
