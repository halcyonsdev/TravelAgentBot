package com.halcyon.travelagent.api.yandex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.halcyon.travelagent.api.geoapify.Coordinate;
import com.halcyon.travelagent.api.geoapify.GeoapifyAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.halcyon.travelagent.config.Credentials.getYandexRaspApiKey;

@Component
@RequiredArgsConstructor
public class YandexAPI {
    private final RestTemplate restTemplate;
    private final GeoapifyAPI geoapifyAPI;

    private static final String API_KEY = getYandexRaspApiKey();
    private static final String NEAREST_STATIONS_URL = "https://api.rasp.yandex.net/v3.0/nearest_stations";
    private static final String SEARCH_URL = "https://api.rasp.yandex.net/v3.0/search";

    public Optional<List<Station>> getNearestStations(String city) {
        Optional<Coordinate> cityCoordinateOptional = geoapifyAPI.getRussianCityCoordinate(city);

        if (cityCoordinateOptional.isEmpty()) {
            return Optional.empty();
        }

        return sendGetNearestStationsRequest(cityCoordinateOptional.get().getLatitude(), cityCoordinateOptional.get().getLongitude());
    }

    private Optional<List<Station>> sendGetNearestStationsRequest(String latitude, String longitude) {
        String nearestStationsUrl = String.format(
                "%s?format=json&lat=%s&lng=%s&distance=50&station_types=train_station, station&lang=ru_RU&apikey=%s",
                NEAREST_STATIONS_URL, latitude, longitude, API_KEY
        );

        String jsonResponse = restTemplate.getForObject(nearestStationsUrl, String.class);

        if (jsonResponse == null) {
            return Optional.empty();
        }

        JsonArray stationsResponse = JsonParser.parseString(jsonResponse).getAsJsonObject().get("stations").getAsJsonArray();
        List<Station> nearestStations = new ArrayList<>();

        for (JsonElement station : stationsResponse) {
            String title = station.getAsJsonObject().get("title").getAsString();
            String code = station.getAsJsonObject().get("code").getAsString();

            nearestStations.add(new Station(title, code));
        }

        return Optional.of(nearestStations);
    }

    public Optional<String> getTrips(String startStationCode, String destinationStationCode, String date, int order) {
        String searchTripsUrl = String.format(
                "%s?from=%s&to=%s&date=%s&apikey=%s&format=json&lang=ru_RU&page=1&limit=10",
                SEARCH_URL, startStationCode, destinationStationCode, date, API_KEY
        );

        String jsonResponse;
        try {
            jsonResponse = restTemplate.getForObject(searchTripsUrl, String.class);
        } catch (Exception e) {
            return Optional.empty();
        }

        if (jsonResponse == null) {
            return Optional.empty();
        }

        JsonArray segments = JsonParser.parseString(jsonResponse).getAsJsonObject().get("segments").getAsJsonArray();

        if (segments.isEmpty()) {
            return Optional.of("*К сожалению, не удалось найти рейсов с введенными данными*");
        }

        StringBuilder trips = new StringBuilder("*Найденные рейсы:*\n\n");

        for (int i = order; i < order + 3 && i < segments.size(); i++) {
            JsonObject segment = segments.get(i).getAsJsonObject();
            JsonObject thread = segment.get("thread").getAsJsonObject();

            String info = String.format("""
                    ℹ *%s*
                    🕓 _%s_
                    🚊 *%s*
                    📤 *Откуда:* %s
                    📥 *Куда:* %s
                    🔗 *Номер рейса:* %s
                    
                    """,
                    thread.get("title").getAsString(),
                    date,
                    thread.get("carrier").getAsJsonObject().get("title").getAsString(),
                    segment.get("from").getAsJsonObject().get("title").getAsString(),
                    segment.get("to").getAsJsonObject().get("title").getAsString(),
                    thread.get("number").getAsString()
            );

            trips.append(info);
        }

        return Optional.of(trips.toString());
    }
}
