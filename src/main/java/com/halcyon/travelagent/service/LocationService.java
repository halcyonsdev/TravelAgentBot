package com.halcyon.travelagent.service;

import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final TravelService travelService;

    public Location createLocation(String name, Instant startTime, Instant endTime, long travelId) {
        return locationRepository.save(
                Location.builder()
                        .name(name)
                        .street("отсутствует")
                        .startTime(startTime)
                        .endTime(endTime)
                        .travel(travelService.findById(travelId))
                        .build()
        );
    }

    public List<Location> getTravelLocations(long travelId) {
        return locationRepository.findLocationsByTravelIdOrderByStartTime(travelId);
    }
}
