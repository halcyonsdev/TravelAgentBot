package com.halcyon.travelagent.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheManager {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveChatStatus(long chatId, ChatStatus chatStatus) {
        redisTemplate.opsForValue().set(String.valueOf(chatId), chatStatus);
        log.info("Cached status {} for chat with id: {}", chatStatus.getType(), chatId);
    }

    public long saveLocation(String location) {
        long locationId = redisTemplate.opsForValue().increment("location_id");
        String key = "location:" + locationId;

        redisTemplate.opsForValue().set(key, location, Duration.ofDays(1));
        log.info("Cached location \"{}\" with key: {} for one day", location, key);

        return locationId;
    }

    public void remove(String key) {
        redisTemplate.delete(String.valueOf(key));
    }

    public <T> Optional<T> fetch(String key, Class<T> targetClass) {
        Optional<Object> valueOptional = Optional.ofNullable(redisTemplate.opsForValue().get(key));

        if (valueOptional.isEmpty()) {
            log.info("No cached value found for key = {}", key);
            return Optional.empty();
        }

        T value = objectMapper.convertValue(valueOptional.get(), targetClass);
        log.info("Fetched value = {} for key = {}", value, key);

        return Optional.of(value);
    }
}
