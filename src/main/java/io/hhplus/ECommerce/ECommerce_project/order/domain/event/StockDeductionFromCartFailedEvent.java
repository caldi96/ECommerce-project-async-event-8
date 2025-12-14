package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import java.util.List;
import java.util.Map;

/**
 * 장바구니 DB 재고 차감 실패 이벤트
 * - Redis 재고 복구 필요
 */
public record StockDeductionFromCartFailedEvent(
        Long userId,
        List<Map.Entry<Long, Integer>> sortedEntries,  // productId, quantity
        String failureReason
) {
    public static StockDeductionFromCartFailedEvent of(
            Long userId,
            List<Map.Entry<Long, Integer>> sortedEntries,
            String failureReason
    ) {
        return new StockDeductionFromCartFailedEvent(userId, sortedEntries, failureReason);
    }
}
