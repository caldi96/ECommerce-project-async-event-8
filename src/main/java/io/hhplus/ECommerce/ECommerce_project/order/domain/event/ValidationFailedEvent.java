package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

public record ValidationFailedEvent(
        Long productId,
        int quantity,
        String failureReason
) {
    public static ValidationFailedEvent of(Long productId, int quantity, String failureReason) {
        return new ValidationFailedEvent(productId, quantity, failureReason);
    }
}
