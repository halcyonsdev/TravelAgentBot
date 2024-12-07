package com.halcyon.travelagent.service;

import com.halcyon.travelagent.entity.Route;
import com.halcyon.travelagent.entity.RoutePoint;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.exception.RouteNotFoundException;
import com.halcyon.travelagent.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;

    public Route createRoute(String routeName, String startPointName, String destinationPointName, String apiMapUrl, Travel travel) {
        Route route = routeRepository.save(new Route(routeName, apiMapUrl, travel));

        RoutePoint destinationPoint = RoutePoint.builder()
                .name(destinationPointName)
                .route(route)
                .build();

        RoutePoint startPoint = RoutePoint.builder()
                .name(startPointName)
                .route(route)
                .nextPoint(destinationPoint)
                .build();

        route.setStartPoint(startPoint);
        route.setDestinationPoint(destinationPoint);

        return routeRepository.save(route);
    }

    public Route findById(long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException("Route with id=" + routeId + " not found."));
    }

    public List<Route> getTravelRoutes(long travelId) {
        return routeRepository.findRoutesByTravelId(travelId);
    }

    public Travel deleteRouteAndGetTravel(long routeId) {
        Route route = findById(routeId);
        routeRepository.delete(route);
        return route.getTravel();
    }
}
