package com.halcyon.travelagent.repository;

import com.halcyon.travelagent.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findLocationsByTravelIdOrderByStartTime(long travelId);
}
