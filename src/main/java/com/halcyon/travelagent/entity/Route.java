package com.halcyon.travelagent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "api_map_url")
    private String apiMapUrl;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "start_point_id", referencedColumnName = "id")
    private RoutePoint startPoint;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "destination_point_id", referencedColumnName = "id")
    private RoutePoint destinationPoint;

    @ManyToOne
    @JoinColumn(name = "travel_id", referencedColumnName = "id")
    private Travel travel;

    public Route(String name, String apiMapUrl, Travel travel) {
        this.name = name;
        this.apiMapUrl = apiMapUrl;
        this.travel = travel;
    }
}
