package com.halcyon.travelagent.api.geoapify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.halcyon.travelagent.entity.Route;
import com.halcyon.travelagent.entity.RoutePoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.config.Credentials.getGeoapifyApiKey;

@Component
@RequiredArgsConstructor
public class GeoapifyAPI {
    private final RestTemplate restTemplate;

    private static final String API_KEY = getGeoapifyApiKey();
    private static final String GEOCODE_URL = "https://api.geoapify.com/v1/geocode/search";
    private static final String ROUTING_URL = "https://api.geoapify.com/v1/routing";
    private static final String STATICMAP_URL = "https://maps.geoapify.com/v1/staticmap";

    public Optional<List<String>> getLocations(String request) {
        String jsonResponse = restTemplate.getForObject(String.format("%s?city=%s&lang=ru&apiKey=%s", GEOCODE_URL, request, API_KEY), String.class);
        if (jsonResponse == null) {
            return Optional.empty();
        }

        JsonArray features = JsonParser.parseString(jsonResponse).getAsJsonObject().get("features").getAsJsonArray();
        if (features.isEmpty()) {
            return Optional.empty();
        }

        List<String> cities = new ArrayList<>();
        for (JsonElement feature : features) {
            cities.add(feature.getAsJsonObject().get("properties").getAsJsonObject().get("formatted").getAsString());
        }

        return Optional.of(cities);
    }

    public Optional<String> getStreet(String request, String text) {
        String url = String.format("%s?text=%s&street=%s&lang=ru&apiKey=%s", GEOCODE_URL, text, request, API_KEY);
        Optional<JsonArray> features = getLocationFeatures(url);

        if (features.isEmpty()) {
            return Optional.empty();
        }

        for (JsonElement feature : features.get()) {
            JsonObject properties = feature.getAsJsonObject().get("properties").getAsJsonObject();

            if (properties.has("name")) {
                return Optional.of(properties.get("name").getAsString());
            }
        }

        return Optional.empty();
    }

    public Optional<JsonArray> getLocationFeatures(String url) {
        String jsonResponse = restTemplate.getForObject(url, String.class);

        if (jsonResponse == null) {
            return Optional.empty();
        }

        try {
            JsonArray features = JsonParser.parseString(jsonResponse).getAsJsonObject().get("features").getAsJsonArray();

            if (features.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(features);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> getNewRouteImageUrl(String startPointName, String destinationPointName) {
        Optional<Coordinate> startCoordinatesOptional = getLocationCoordinate(getLocationCoordinateUrl(startPointName));
        Optional<Coordinate> destinationCoordinatesOptional = getLocationCoordinate(getLocationCoordinateUrl(destinationPointName));

        if (startCoordinatesOptional.isEmpty() || destinationCoordinatesOptional.isEmpty()) {
            return Optional.empty();
        }

        Coordinate startPointCoordinate = startCoordinatesOptional.get();
        Coordinate destinationPointCoordinate = destinationCoordinatesOptional.get();

        Optional<String> routeCoordinatesOptional = getRoute(List.of(startPointCoordinate, destinationPointCoordinate));

        if (routeCoordinatesOptional.isEmpty()) {
            return Optional.empty();
        }

        String url = String.format(
                "%s?style=klokantech-basic&width=1200&height=700&type=short&geometry=polyline:%s;linewidth:2&marker=%s|%s&apiKey=%s",
                STATICMAP_URL, routeCoordinatesOptional.get(), getMarker(startPointCoordinate, 1), getMarker(destinationPointCoordinate, 2), API_KEY
        );

        return Optional.of(url);
    }

    private String getLocationCoordinateUrl(String text) {
        String[] data = text.split(", улица ");
        String url = String.format("%s?text=%s&lang=ru&apiKey=%s", GEOCODE_URL, text, API_KEY);

        if (data.length == 2) {
            url = String.format("%s?text=%s&street=%s&lang=ru&apiKey=%s", GEOCODE_URL, data[0], data[1], API_KEY);
        }

        return url;
    }

    private Optional<Coordinate> getLocationCoordinate(String url) {
        Optional<JsonArray> features = getLocationFeatures(url);

        if (features.isEmpty()) {
            return Optional.empty();
        }

        JsonObject properties = features.get().get(0).getAsJsonObject().get("properties").getAsJsonObject();
        String longitude = properties.get("lon").getAsString();
        String latitude = properties.get("lat").getAsString();

        return Optional.of(new Coordinate(longitude, latitude));
    }

    private String getMarker(Coordinate pointCoordinate, int number) {
        return String.format(
                "lonlat:%s,%s;type:material;size:large;icon:cloud;icontype:awesome;text:%s;whitecircle:no",
                pointCoordinate.getLongitude(), pointCoordinate.getLatitude(), number
        );
    }

    public Optional<InputFile> getRouteImageFile(String url) {
        ResponseEntity<byte[]> routeImageResponse = restTemplate.getForEntity(url, byte[].class);

        if (routeImageResponse.getStatusCode().is2xxSuccessful() && routeImageResponse.getBody() != null) {
            byte[] fileData = routeImageResponse.getBody();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);

            InputFile routeImageFile = new InputFile();
            routeImageFile.setMedia(inputStream, "route.jpg");

            return Optional.of(routeImageFile);
        }

        return Optional.empty();
    }

    public Optional<String> getRoute(List<Coordinate> pointsCoordinates) {
        StringBuilder coordinatesText = new StringBuilder();
        for (Coordinate coordinate : pointsCoordinates) {
            coordinatesText.append(String.format("%s,%s|", coordinate.getLatitude(), coordinate.getLongitude()));
        }

        String url = String.format(
                "%s?waypoints=%s&mode=drive&type=short&apiKey=%s",
                ROUTING_URL,
                coordinatesText.substring(0, coordinatesText.length() - 1),
                API_KEY
        );

        Optional<JsonArray> features = getLocationFeatures(url);
        if (features.isEmpty()) {
            return Optional.empty();
        }

        JsonArray coordinates = features.get().get(0).getAsJsonObject().get("geometry").getAsJsonObject().get("coordinates").getAsJsonArray();
        StringBuilder routeCoordinates = new StringBuilder();

        for (int i = 0; i < coordinates.size(); i++) {
            int counter = 0;
            for (JsonElement coordinate : coordinates.get(i).getAsJsonArray()) {
                counter++;

                if (counter % getLinkDel(coordinates.get(i).getAsJsonArray().size()) == 0) {
                    JsonArray step = coordinate.getAsJsonArray();
                    String latitude = step.get(0).getAsString();
                    String longitude = step.get(1).getAsString();

                    routeCoordinates.append(latitude).append(",").append(longitude).append(",");
                }
            }
        }

        return Optional.of(routeCoordinates.substring(0, routeCoordinates.length() - 1));
    }

    private int getLinkDel(int count) {
        if (count < 60) {
            return 1;
        }

        return (int) Math.ceil(count / 60);
    }

    public Optional<String> getUpdatedRouteImageUrl(Route route, long routePointId, String newPointName, boolean isDeleted) {
        List<Coordinate> pointsCoordinates = new ArrayList<>();
        RoutePoint currentPoint = route.getStartPoint();

        StringBuilder markers = new StringBuilder();
        int number = 1;

        if (routePointId == -1) {
            Optional<Coordinate> newRouteCoordinateOptional = getLocationCoordinate(getLocationCoordinateUrl(newPointName));

            if (newRouteCoordinateOptional.isEmpty()) {
                return Optional.empty();
            }

            pointsCoordinates.add(newRouteCoordinateOptional.get());
            markers.append(getMarker(newRouteCoordinateOptional.get(), number++)).append("|");
        }

        while (currentPoint != null) {
            Optional<Coordinate> coordinateOptional = getLocationCoordinate(getLocationCoordinateUrl(currentPoint.getName()));

            if (coordinateOptional.isEmpty()) {
                return Optional.empty();
            }

            if (currentPoint.getId() == routePointId && isDeleted) {
                currentPoint = currentPoint.getNextPoint();
                continue;
            }

            pointsCoordinates.add(coordinateOptional.get());
            markers.append(getMarker(coordinateOptional.get(), number++)).append("|");

            if (currentPoint.getId() == routePointId) {
                Optional<Coordinate> newRouteCoordinateOptional = getLocationCoordinate(getLocationCoordinateUrl(newPointName));

                if (newRouteCoordinateOptional.isEmpty()) {
                    return Optional.empty();
                }

                pointsCoordinates.add(newRouteCoordinateOptional.get());
                markers.append(getMarker(newRouteCoordinateOptional.get(), number++)).append("|");
            }

            currentPoint = currentPoint.getNextPoint();
        }

        Optional<String> coordinatesTextOptional = getRoute(pointsCoordinates);

        if (coordinatesTextOptional.isEmpty()) {
            return Optional.empty();
        }

        String url = String.format(
                "%s?style=klokantech-basic&width=1200&height=700&type=short&geometry=polyline:%s;linewidth:2&marker=%s&apiKey=%s",
                STATICMAP_URL,
                coordinatesTextOptional.get(),
                markers.substring(0, markers.length() - 1),
                API_KEY
        );

        return Optional.of(url);
    }

    public Optional<Coordinate> getRussianCityCoordinate(String city) {
        String url = String.format("%s?city=%s&country=Россия&lang=ru&apiKey=%s", GEOCODE_URL, city, API_KEY);
        return getLocationCoordinate(url);
    }
}
