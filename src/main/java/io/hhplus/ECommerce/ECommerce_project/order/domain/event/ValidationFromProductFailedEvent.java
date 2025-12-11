package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

public record ValidationFromProductFailedEvent(
        Long productId,
        int quantity,
        String failureReason
) {
    public static ValidationFromProductFailedEvent of(Long productId, int quantity, String failureReason) {
        return new ValidationFromProductFailedEvent(productId, quantity, failureReason);
    }
}
