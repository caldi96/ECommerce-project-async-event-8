package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

public record OrderCancelEvent(
        Long orderId,
        Long userId
) {
    public static OrderCancelEvent of (Long orderId, Long userId) {
        return new OrderCancelEvent(orderId, userId);
    }
}
