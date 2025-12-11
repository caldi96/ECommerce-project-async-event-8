package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

public record StockDeductionFailedEvent(
        Long productId,
        int quantity,
        String failureReason
) {
    public static StockDeductionFailedEvent of(Long productId, int quantity, String failureReason) {
        return new StockDeductionFailedEvent(productId, quantity, failureReason);
    }
}
