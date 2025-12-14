package io.hhplus.ECommerce.ECommerce_project.coupon.domain.event;

/**
 * 쿠폰 발급 이벤트
 * - Redis 발급 성공 후 발행
 * - DB 저장 및 수량 증가를 위한 비동기 이벤트
 */
public record CouponIssuedEvent(
        Long userId,
        Long couponId
) {
    public static CouponIssuedEvent of(Long userId, Long couponId) {
        return new CouponIssuedEvent(userId, couponId);
    }
}
