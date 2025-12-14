package io.hhplus.ECommerce.ECommerce_project.coupon.domain.event;

/**
 * 쿠폰 검증 이벤트
 * - Redis 발급 성공 쿠폰 유효성 검증(활성화 상태, 사용 기간)
 * - 레디스에서 발급 받은 쿠폰의 유효성 검사를 위한 비동기 이벤트
 */
public record CouponValidationEvent(
        Long userId,
        Long couponId
) {
    public static CouponValidationEvent of(Long userId, Long couponId) {
        return new CouponValidationEvent(userId, couponId);
    }
}
