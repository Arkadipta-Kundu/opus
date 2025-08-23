package org.arkadipta.opus.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplate() {
        // Create a mock RedisTemplate for testing
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Use in-memory cache manager for testing instead of Redis
        return new ConcurrentMapCacheManager();
    }
}
