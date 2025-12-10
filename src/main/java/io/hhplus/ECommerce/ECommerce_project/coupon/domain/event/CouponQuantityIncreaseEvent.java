package io.hhplus.ECommerce.ECommerce_project.coupon.domain.event;

/**
 * 쿠폰 수량 증가 이벤트
 * - UserCoupon 저장 성공 후 발행
 * - DB의 issuedQuantity를 증가시키기 위한 이벤트
 */
public record CouponQuantityIncreaseEvent(
        Long couponId
) {
    public static CouponQuantityIncreaseEvent of(Long couponId) {
        return new CouponQuantityIncreaseEvent(couponId);
    }
}
