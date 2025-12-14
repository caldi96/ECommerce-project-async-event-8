package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Redis 쿠폰 메타데이터 서비스
 * - 쿠폰 기본 정보를 Redis에 캐싱하여 빠른 조회 제공
 * - Hash 구조로 저장: coupon:metadata:{couponId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponMetadataService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_METADATA_PREFIX = "coupon:metadata:";
    private static final long TTL_DAYS = 30;

    /**
     * 쿠폰 메타데이터 저장 (Hash 구조)
     */
    public void saveCouponMetadata(
            Long couponId,
            int totalQuantity,
            boolean isActive,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        String key = COUPON_METADATA_PREFIX + couponId;

        redisTemplate.opsForHash().put(key, "totalQuantity", String.valueOf(totalQuantity));
        redisTemplate.opsForHash().put(key, "isActive", String.valueOf(isActive));
        redisTemplate.opsForHash().put(key, "startDate", startDate.toString());
        redisTemplate.opsForHash().put(key, "endDate", endDate.toString());

        redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));

        log.info("쿠폰 메타데이터 저장 완료 - couponId: {}, totalQuantity: {}, isActive: {}",
                couponId, totalQuantity, isActive);
    }

    /**
     * 쿠폰 총 수량 조회
     */
    public Integer getTotalQuantity(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        Object value = redisTemplate.opsForHash().get(key, "totalQuantity");

        if (value == null) {
            log.debug("Redis에 쿠폰 메타데이터 없음 - couponId: {}", couponId);
            return null;
        }

        return Integer.parseInt(value.toString());
    }

    /**
     * 쿠폰 활성화 여부 조회
     */
    public Boolean isActive(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        Object value = redisTemplate.opsForHash().get(key, "isActive");

        if (value == null) {
            return null;
        }

        return Boolean.parseBoolean(value.toString());
    }

    /**
     * 쿠폰 시작일 조회
     */
    public LocalDateTime getStartDate(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        Object value = redisTemplate.opsForHash().get(key, "startDate");

        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value.toString());
    }

    /**
     * 쿠폰 종료일 조회
     */
    public LocalDateTime getEndDate(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        Object value = redisTemplate.opsForHash().get(key, "endDate");

        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value.toString());
    }

    /**
     * 쿠폰 메타데이터 전체 조회
     */
    public Map<Object, Object> getCouponMetadata(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 쿠폰 메타데이터 삭제
     */
    public void deleteCouponMetadata(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        redisTemplate.delete(key);
        log.info("쿠폰 메타데이터 삭제 완료 - couponId: {}", couponId);
    }

    /**
     * 쿠폰 메타데이터 존재 여부 확인
     */
    public boolean exists(Long couponId) {
        String key = COUPON_METADATA_PREFIX + couponId;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * totalQuantity만 업데이트
     */
    public void updateTotalQuantity(Long couponId, int totalQuantity) {
        String key = COUPON_METADATA_PREFIX + couponId;

        if (!exists(couponId)) {
            log.warn("쿠폰 메타데이터가 존재하지 않아 업데이트 불가 - couponId: {}", couponId);
            return;
        }

        redisTemplate.opsForHash().put(key, "totalQuantity", String.valueOf(totalQuantity));
        log.info("쿠폰 수량 업데이트 완료 - couponId: {}, totalQuantity: {}", couponId, totalQuantity);
    }

    /**
     * isActive만 업데이트
     */
    public void updateIsActive(Long couponId, boolean isActive) {
        String key = COUPON_METADATA_PREFIX + couponId;

        if (!exists(couponId)) {
            log.warn("쿠폰 메타데이터가 존재하지 않아 업데이트 불가 - couponId: {}", couponId);
            return;
        }

        redisTemplate.opsForHash().put(key, "isActive", String.valueOf(isActive));
        log.info("쿠폰 활성화 상태 업데이트 완료 - couponId: {}, isActive: {}", couponId, isActive);
    }
}