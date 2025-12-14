package io.hhplus.ECommerce.ECommerce_project.coupon.domain.event;

public record UserValidationEvent(
        Long userId,
        Long couponId
) {
    public static UserValidationEvent of(Long userId, Long couponId) {
        return new UserValidationEvent(userId, couponId);
    }
}
