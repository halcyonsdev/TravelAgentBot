package com.halcyon.travelagent.api.openweathermap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static com.halcyon.travelagent.config.Credentials.getOpenWeatherMapApiKey;

@Component
@RequiredArgsConstructor
public class OpenWeatherMapAPI {
    private final RestTemplate restTemplate;

    private static final String API_KEY = getOpenWeatherMapApiKey();
    private static final String FORECAST_API_URL = "https://api.openweathermap.org/data/2.5/forecast";

    public Optional<String> getCityWeatherInfo(String city, int order) {
        String jsonResponse = restTemplate.getForObject(String.format(
                "%s?q=%s&&appid=%s&units=metric&lang=ru",
                FORECAST_API_URL, city, API_KEY), String.class);

        if (jsonResponse == null) {
            return Optional.empty();
        }

        JsonArray forecastList = JsonParser.parseString(jsonResponse).getAsJsonObject().get("list").getAsJsonArray();

        if (forecastList.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder forecasts = new StringBuilder(String.format("*Погода в городе \"%s\"*%n%n", city));

        for (int i = order + 1; i < order + 4 && i < forecastList.size(); i++) {
            forecasts.append(getForecast(forecastList.get(i).getAsJsonObject()));
        }

        return Optional.of(forecasts.toString());
    }

    private String getForecast(JsonObject forecast) {
        JsonObject main = forecast.get("main").getAsJsonObject();

        String temperature = main.get("temp").getAsString();
        String feelsLike = main.get("feels_like").getAsString();
        String minTemperature = main.get("temp_min").getAsString();
        String maxTemperature = main.get("temp_max").getAsString();
        String humidity = main.get("humidity").getAsString();

        String time = forecast.get("dt_txt").getAsString();
        String windSpeed = forecast.get("wind").getAsJsonObject().get("speed").getAsString();
        String description = forecast.get("weather").getAsJsonArray().get(0).getAsJsonObject().get("description").getAsString();

        return String.format("""
                🕐 *%s*
                ℹ️ _%s_
                🌡 *Температура:* %s ℃ (от %s ℃ до %s ℃)
                👀 *Чувствуется как:* %s ℃
                💨 *Скорость ветра:* %s м/с
                💧 *Влажность: %s%%*
                
                """,
                time,
                description,
                temperature, minTemperature, maxTemperature,
                feelsLike,
                windSpeed,
                humidity
        );
    }
}
