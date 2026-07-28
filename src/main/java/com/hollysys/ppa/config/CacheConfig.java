package com.hollysys.ppa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache + Redis 配置
 * 支持按 cache name 设置不同 TTL
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${alarm.stats.cache-ttl:300}")
    private long statsTtlSeconds;

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 统计类缓存 TTL 5 分钟，设备下拉 30 分钟
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("alarm:stats:overview", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));
        cacheConfigs.put("alarm:stats:specialty", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));
        cacheConfigs.put("alarm:stats:level", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));
        cacheConfigs.put("alarm:stats:type", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));
        cacheConfigs.put("alarm:stats:topFreq", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));
        cacheConfigs.put("alarm:stats:topDur", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));
        cacheConfigs.put("alarm:stats:dailyTrend", defaultConfig.entryTtl(Duration.ofSeconds(statsTtlSeconds)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
