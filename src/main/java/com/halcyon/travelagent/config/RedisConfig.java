package com.halcyon.travelagent.config;

import com.halcyon.travelagent.caching.ChatStatus;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        var standaloneConfiguration = new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
        standaloneConfiguration.setPassword(redisProperties.getPassword());
        return new LettuceConnectionFactory(standaloneConfiguration);
    }

    @Bean
    public RedisTemplate<Long, ChatStatus> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Long, ChatStatus> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(ChatStatus.class));
        return redisTemplate;
    }
}
