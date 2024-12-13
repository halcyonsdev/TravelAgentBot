package com.halcyon.travelagent.service;

import com.halcyon.travelagent.entity.Location;
import com.halcyon.travelagent.exception.LocationNotFoundException;
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
        return save(
                Location.builder()
                        .name(name)
                        .street("отсутствует")
                        .startTime(startTime)
                        .endTime(endTime)
                        .travel(travelService.findById(travelId))
                        .build()
        );
    }

    public int getTravelLocationsCount(long travelId) {
        return locationRepository.countLocationsByTravelId(travelId);
    }

    public Location save(Location location) {
        return locationRepository.save(location);
    }

    public long deleteLocationAndGetTravelId(long locationId) {
        Location location = findById(locationId);
        locationRepository.delete(location);

        return location.getTravel().getId();
    }

    public Location findById(long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException("Location with id=" + locationId + " not found."));
    }

    public List<Location> getTravelLocations(long travelId) {
        return locationRepository.findLocationsByTravelIdOrderByStartTime(travelId);
    }

    public Location changeName(long locationId, String newLocationName) {
        Location location = findById(locationId);
        location.setName(newLocationName);

        return save(location);
    }

    public Location changeStartTime(long locationId, Instant startTime) {
        Location location = findById(locationId);
        location.setStartTime(startTime);

        return save(location);
    }

    public Location changeEndTime(long locationId, Instant endTime) {
        Location location = findById(locationId);
        location.setEndTime(endTime);

        return save(location);
    }
}
