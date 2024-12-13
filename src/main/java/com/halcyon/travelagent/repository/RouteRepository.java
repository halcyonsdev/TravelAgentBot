package com.halcyon.travelagent.repository;

import com.halcyon.travelagent.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findRoutesByTravelId(long travelId);

    @Query("SELECT COUNT(*) FROM Route r WHERE r.travel.id = :travelId")
    int countRoutesByTravelId(@Param("travelId") long travelId);
}
