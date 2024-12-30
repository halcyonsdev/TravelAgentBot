package com.halcyon.travelagent.api.yandex;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CityArea {
    private String city;
    private String minLatitude;
    private String minLongitude;
    private String maxLatitude;
    private String maxLongitude;
}
