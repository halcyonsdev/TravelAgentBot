package com.halcyon.travelagent.api.sightsafari;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.halcyon.travelagent.api.geoapify.Coordinate;
import com.halcyon.travelagent.api.yandex.CityArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SightSafariAPI {
    private final RestTemplate restTemplate;

    private static final String SIGHTS_URL = "https://sightsafari.city/api/v1/geography/sights";

    public Optional<List<SightInfo>> getCitySightsInfo(CityArea cityArea, int order) {
        String url = String.format(
                "%s?minLat=%s&minLon=%s&maxLat=%s&maxLon=%s",
                SIGHTS_URL, cityArea.getMinLatitude(), cityArea.getMinLongitude(),
                cityArea.getMaxLatitude(), cityArea.getMaxLongitude()
        );

        String jsonResponse = restTemplate.getForObject(url, String.class);

        if (jsonResponse == null) {
            return Optional.empty();
        }

        JsonArray sights = JsonParser.parseString(jsonResponse).getAsJsonObject().get("body").getAsJsonArray();
        List<SightInfo> sightsInfo = new ArrayList<>();

        for (int i = order; i < order + 5 && i < sights.size(); i++) {
            sightsInfo.add(getSightInfo(sights.get(i).getAsJsonObject()));
        }

        return Optional.of(sightsInfo);
    }

    private SightInfo getSightInfo(JsonObject sight) {
        return SightInfo.builder()
                .name(sight.get("name").getAsString())
                .type(getSightType(sight))
                .links(getLinks(sight))
                .coordinate(getSightCoordinate(sight))
                .build();
    }

    private String getSightType(JsonObject sight) {
        switch (sight.get("type").getAsString()) {
            case "TOURISM" -> {
                return "(_Туристические достопримечательности_)";
            }
            case "WATER" -> {
                return "(_Реки, каналы, озера и их набережные_)";
            }
            case "PARK" -> {
                return "(_Зеленые насаждения - парки, скверы, бульвары_)";
            }
            case "MONUMENT" -> {
                return "(_Памятник_)";
            }
            case "RELIGIOUS_OBJECT" -> {
                return "(_Религиозный объект_)";
            }
            case "PEDESTRIAN_AREA" -> {
                return "(_Пешеходные улицы и площади_)";
            }
            case "NEGATIVE" -> {
                return "(_Промзоны, стройплощадоки и т.п., рядом с которыми пешеходам некомфортно_)";
            }
            case "ADVERTISING" -> {
                return "(_Рекламируемый объект_)";
            }
            default -> {
                return "";
            }
        }
    }

    private String getLinks(JsonObject sight) {
        if (!sight.has("links") || sight.get("links").getAsJsonArray().isEmpty()) {
            return "";
        }

        StringBuilder links = new StringBuilder();
        for (JsonElement link : sight.get("links").getAsJsonArray()) {
            links.append(String.format("\uD83D\uDD17 %s%n", link.getAsString().replace(" ", "%20")));
        }

        return links.toString();
    }

    private Coordinate getSightCoordinate(JsonObject sight) {
        JsonArray coordinate = sight.get("coordinates").getAsJsonArray().get(0).getAsJsonArray();
        return new Coordinate(coordinate.get(1).getAsString(), coordinate.get(0).getAsString());
    }
}
