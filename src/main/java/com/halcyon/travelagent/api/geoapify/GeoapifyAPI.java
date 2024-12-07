package com.halcyon.travelagent.api.geoapify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        JsonArray features = JsonParser.parseString(jsonResponse).getAsJsonObject().get("features").getAsJsonArray();
        if (features.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(features);
    }

    public Optional<String> getRouteImageUrl(String startPointName, String destinationPointName) {
        Optional<String> routeCoordinatesOptional = getRoute(startPointName, destinationPointName);

        if (routeCoordinatesOptional.isEmpty()) {
            return Optional.empty();
        }

        String url = String.format(
                "%s?style=klokantech-basic&width=1200&height=700&type=short&geometry=polyline:%s;linewidth:2&apiKey=%s",
                STATICMAP_URL, routeCoordinatesOptional.get(), API_KEY
        );

        return Optional.of(url);
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

    public Optional<String> getRoute(String startPointName, String destinationPointName) {
        Optional<double[]> startPointCoordinates = getLocationCoordinates(startPointName);
        Optional<double[]> destinationPointCoordinates = getLocationCoordinates(destinationPointName);

        if (startPointCoordinates.isEmpty() || destinationPointCoordinates.isEmpty()) {
            return Optional.empty();
        }

        String url = String.format(
                "%s?waypoints=%s,%s|%s,%s&mode=drive&type=short&apiKey=%s",
                ROUTING_URL,
                startPointCoordinates.get()[1], startPointCoordinates.get()[0],
                destinationPointCoordinates.get()[1], destinationPointCoordinates.get()[0],
                API_KEY
        );

        Optional<JsonArray> features = getLocationFeatures(url);
        if (features.isEmpty()) {
            return Optional.empty();
        }

        JsonArray coordinates = features.get().get(0).getAsJsonObject().get("geometry").getAsJsonObject().get("coordinates").getAsJsonArray().get(0).getAsJsonArray();
        StringBuilder routeCoordinates = new StringBuilder();

        int counter = 0;
        for (JsonElement coordinate : coordinates) {
            counter++;

            if (counter % getLinkDel(coordinates.size()) == 0) {
                JsonArray step = coordinate.getAsJsonArray();
                String latitude = step.get(0).getAsString();
                String longitude = step.get(1).getAsString();

                routeCoordinates.append(latitude).append(",").append(longitude).append(",");
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

    public Optional<double[]> getLocationCoordinates(String text) {
        String[] data = text.split(", улица ");
        String url = String.format("%s?text=%s&lang=ru&apiKey=%s", GEOCODE_URL, text, API_KEY);

        if (data.length == 2) {
            url = String.format("%s?text=%s&street=%s&lang=ru&apiKey=%s", GEOCODE_URL, data[0], data[1], API_KEY);
        }

        Optional<JsonArray> features = getLocationFeatures(url);

        if (features.isEmpty()) {
            return Optional.empty();
        }

        JsonObject properties = features.get().get(0).getAsJsonObject().get("properties").getAsJsonObject();
        double longitude = properties.get("lon").getAsDouble();
        double latitude = properties.get("lat").getAsDouble();

        return Optional.of(new double[] {longitude, latitude});
    }
}
