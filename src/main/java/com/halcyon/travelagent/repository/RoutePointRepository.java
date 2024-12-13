package com.halcyon.travelagent.repository;

import com.halcyon.travelagent.entity.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM RoutePoint rp WHERE rp.route.id = :routeId")
    void deleteAllByRouteId(@Param("routeId") long routeId);

    @Query("SELECT COUNT(*) FROM RoutePoint rp WHERE rp.route.id = :routeId")
    int countRoutePointsByRouteId(@Param("routeId") long routeId);
}
