package io.hhplus.ECommerce.ECommerce_project.coupon.domain.event;

/**
 * 쿠폰 발급 실패 이벤트
 * - DB 저장 실패 시 발행
 * - Redis 롤백을 위한 보상 트랜잭션 이벤트
 */
public record CouponIssueFailedEvent(
        Long userId,
        Long couponId,
        String failureReason
) {
    public static CouponIssueFailedEvent of(Long userId, Long couponId, String failureReason) {
        return new CouponIssueFailedEvent(userId, couponId, failureReason);
    }
}
