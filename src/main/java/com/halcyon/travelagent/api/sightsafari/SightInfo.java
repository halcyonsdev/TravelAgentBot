package com.halcyon.travelagent.api.sightsafari;

import com.halcyon.travelagent.api.geoapify.Coordinate;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SightInfo {
    private String name;
    private String type;
    private String links;
    private Coordinate coordinate;
}
