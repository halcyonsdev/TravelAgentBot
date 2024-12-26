package com.halcyon.travelagent.api.hotellook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HotelLookAPI {
    private final RestTemplate restTemplate;

    private static final String CACHE_API_URL = "https://engine.hotellook.com/api/v2/cache.json";

    public Optional<String> getCityHotelsInfo(String city, String checkIn, String checkOut, int order) {
        String jsonResponse = restTemplate.getForObject(String.format(
                "%s?location=%s&currency=rub&checkIn=%s&checkOut=%s&limit=10",
                CACHE_API_URL, city, checkIn, checkOut), String.class);

        if (jsonResponse == null) {
            return Optional.empty();
        }

        JsonArray hotels = JsonParser.parseString(jsonResponse).getAsJsonArray();
        StringBuilder hotelsInfoText = new StringBuilder(String.format(
                """
                *Найденнные отели:*
                
                🕐 *Заезд:* _%s_
                🕙 *Выезд:* _%s_
                   
                """,
                checkIn, checkOut
        ));

        int number = order;
        for (int i = order; i < order + 3 && i < hotels.size(); i++) {
            hotelsInfoText.append(String.format("%s. %s", ++number, getHotelInfo(hotels.get(i).getAsJsonObject())));
        }

        return Optional.of(hotelsInfoText.toString());
    }

    private String getHotelInfo(JsonObject hotel) {
        double priceFrom = hotel.get("priceFrom").getAsDouble();
        int stars = hotel.get("stars").getAsInt();
        String hotelName = hotel.get("hotelName").getAsString();

        JsonObject location = hotel.get("location").getAsJsonObject();
        String locationName = location.get("name").getAsString();

        return String.format("""
                ℹ️ *%s*
                    ⭐️ %s
                    💵 *Цены на этот промежуток от:* %s₽
                    🏙 *Местонахождение:* %s
                
                """,
                hotelName,
                stars,
                priceFrom,
                locationName
        );
    }
}
