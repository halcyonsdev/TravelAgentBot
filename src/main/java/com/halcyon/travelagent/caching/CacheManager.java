package com.halcyon.travelagent.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheManager {
    private final RedisTemplate<Long, ChatStatus> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveChatStatus(long userId, ChatStatus chatStatus) {
        redisTemplate.opsForValue().set(userId, chatStatus);
        log.info("Cached chat status {} for user with id: {}", chatStatus.getType(), userId);
    }

    public void deleteChatStatus(long userId) {
        redisTemplate.delete(userId);
    }

    public Optional<ChatStatus> getChatStatus(long userId) {
        Optional<com.halcyon.travelagent.caching.ChatStatus> chatStatusValue = Optional.ofNullable(redisTemplate.opsForValue().get(userId));

        if (chatStatusValue.isEmpty()) {
            log.info("No cached chat status found for user with id: {}", userId);
            return Optional.empty();
        }

        ChatStatus chatStatus = objectMapper.convertValue(chatStatusValue.get(), ChatStatus.class);
        log.info("Fetched cached chat status {} for user with id: {}", chatStatus.getType(), userId);

        return Optional.of(chatStatus);
    }
}
