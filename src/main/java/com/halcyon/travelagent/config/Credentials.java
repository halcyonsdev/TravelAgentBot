package com.halcyon.travelagent.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Credentials {
    private Credentials() {}

    private static final Dotenv dotenv = Dotenv.load();

    public static String getBotToken() {
        return dotenv.get("TELEGRAM_BOT_TOKEN");
    }

    public static String getBotUsername() {
        return dotenv.get("TELEGRAM_BOT_USERNAME");
    }

    public static Long getCreatorId() {
        return Long.parseLong(dotenv.get("CREATOR_ID"));
    }
}
