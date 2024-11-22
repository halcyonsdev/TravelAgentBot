package com.halcyon.travelagent.api.geoapify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.config.Credentials.getGeoapifyApiKey;

@Component
@RequiredArgsConstructor
public class GeoapifyAPI {
    private final RestTemplate restTemplate;

    private static final String API_KEY = getGeoapifyApiKey();
    private static final String GEOCODE_URL = "https://api.geoapify.com/v1/geocode/search?city=%s&lang=ru&apiKey=" + API_KEY;

    public Optional<List<String>> getLocations(String city) {
        String jsonResponse = restTemplate.getForObject(String.format(GEOCODE_URL, city), String.class);
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
}
