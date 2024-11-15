package com.halcyon.travelagent.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    public void saveChatStatus(long chatId, ChatStatus chatStatus) {
        redisTemplate.opsForValue().set(chatId, chatStatus);
        log.info("Cached status {} for chat with id: {}", chatStatus.getType(), chatId);
    }

    public void removeChatStatus(long chatId) {
        redisTemplate.delete(chatId);
    }

    public Optional<ChatStatus> getChatStatus(long chatId) {
        Optional<com.halcyon.travelagent.caching.ChatStatus> chatStatusValue = Optional.ofNullable(redisTemplate.opsForValue().get(chatId));

        if (chatStatusValue.isEmpty()) {
            log.info("No cached status found for chat with id: {}", chatId);
            return Optional.empty();
        }

        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ChatStatus chatStatus = objectMapper.convertValue(chatStatusValue.get(), ChatStatus.class);
        log.info("Fetched cached status {} for chat with id: {}", chatStatus.getType(), chatId);

        return Optional.of(chatStatus);
    }
}
