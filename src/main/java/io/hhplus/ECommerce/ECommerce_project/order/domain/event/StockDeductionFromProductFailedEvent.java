package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

public record StockDeductionFromProductFailedEvent(
        Long productId,
        int quantity,
        String failureReason
) {
    public static StockDeductionFromProductFailedEvent of(Long productId, int quantity, String failureReason) {
        return new StockDeductionFromProductFailedEvent(productId, quantity, failureReason);
    }
}
