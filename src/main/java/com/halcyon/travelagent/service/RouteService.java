package com.halcyon.travelagent.service;

import com.halcyon.travelagent.entity.Route;
import com.halcyon.travelagent.entity.RoutePoint;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.exception.RouteNotFoundException;
import com.halcyon.travelagent.repository.RoutePointRepository;
import com.halcyon.travelagent.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final RoutePointRepository routePointRepository;

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

    @Transactional
    public Route addNewRoutePoint(long routeId, long afterPointId, String newPointName, String newApiMapUrl) {
        Route route = findById(routeId);
        route.setApiMapUrl(newApiMapUrl);
        route = routeRepository.save(route);

        RoutePoint currentPoint = route.getStartPoint();

        while (currentPoint.getId() != afterPointId) {
            currentPoint = currentPoint.getNextPoint();
        }

        RoutePoint newPoint = RoutePoint.builder()
                .name(newPointName)
                .route(route)
                .nextPoint(currentPoint.getNextPoint())
                .build();

        if (currentPoint.getNextPoint() != null) {
            newPoint.setNextPoint(currentPoint.getNextPoint());
            newPoint = routePointRepository.save(newPoint);
        } else {
            route.setDestinationPoint(newPoint);
            route = routeRepository.save(route);
            newPoint = route.getDestinationPoint();
        }
        currentPoint.setNextPoint(newPoint);
        routePointRepository.save(currentPoint);

        return findById(routeId);
    }

    @Transactional
    public Route addNewStartRoutePoint(long routeId, String newPointName, String newApiMapUrl) {
        Route route = findById(routeId);
        route.setApiMapUrl(newApiMapUrl);

        RoutePoint startPoint = route.getStartPoint();

        RoutePoint newStartPoint = RoutePoint.builder()
                .name(newPointName)
                .route(route)
                .nextPoint(startPoint)
                .build();

        route.setStartPoint(newStartPoint);
        return routeRepository.save(route);
    }

    public Route changeName(long routeId, String newRouteName) {
        Route route = findById(routeId);
        route.setName(newRouteName);
        return routeRepository.save(route);
    }
}
