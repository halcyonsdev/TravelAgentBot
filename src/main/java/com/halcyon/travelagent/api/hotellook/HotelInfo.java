package com.halcyon.travelagent.api.hotellook;

import com.halcyon.travelagent.api.geoapify.Coordinate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HotelInfo {
    private String info;
    private Coordinate coordinate;
}
