package com.halcyon.travelagent.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Credentials {
    private Credentials() {}

    private static final Dotenv dotenv = Dotenv.load();

    public static String getBotToken() {
        return dotenv.get("TELEGRAM_BOT_TOKEN");
    }

    public static String getGeoapifyApiKey() {
        return dotenv.get("GEOAPIFY_API_KEY");
    }

    public static String getOpenWeatherMapApiKey() {
        return dotenv.get("OPEN_WEATHER_MAP_API_KEY");
    }
}
