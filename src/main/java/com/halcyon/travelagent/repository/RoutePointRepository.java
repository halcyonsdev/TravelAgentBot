package com.halcyon.travelagent.repository;

import com.halcyon.travelagent.entity.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
}
