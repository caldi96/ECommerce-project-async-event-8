package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import java.util.List;
import java.util.Map;

/**
 * 장바구니 주문 생성 실패 이벤트
 * - DB 재고 + Redis 재고 복구 필요
 */
public record OrderCreationFromCartFailedEvent(
        Long userId,
        List<Map.Entry<Long, Integer>> sortedEntries,  // productId, quantity
        String failureReason,
        boolean needsDbStockRecovery
) {
    public static OrderCreationFromCartFailedEvent of(
            Long userId,
            List<Map.Entry<Long, Integer>> sortedEntries,
            String failureReason,
            boolean needsDbStockRecovery
    ) {
        return new OrderCreationFromCartFailedEvent(userId, sortedEntries, failureReason, needsDbStockRecovery);
    }
}
