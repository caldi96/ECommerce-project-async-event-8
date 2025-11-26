package io.hhplus.ECommerce.ECommerce_project.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 캐시 설정
 * - 분산 환경에서 여러 인스턴스 간 캐시 공유
 * - 영속성이 필요하거나 대용량 데이터 캐싱에 적합
 * - 다중 인스턴스 환경에서 효과적
 */
@Configuration
public class RedisCacheConfig {

    /**
     * Redis 캐시 매니저
     * - 동적으로 캐시 생성 가능 (@Cacheable의 cacheManager 속성으로 지정)
     * - 예: @Cacheable(value = "productList", cacheManager = "redisCacheManager")
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config =
                RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))  // 2분 TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}