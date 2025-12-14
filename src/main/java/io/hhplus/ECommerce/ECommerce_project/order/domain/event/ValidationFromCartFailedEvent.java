package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;

import java.util.List;
import java.util.Map;

/**
 * 장바구니 주문 검증 실패 이벤트
 * - Redis 재고 복구 필요
 */
public record ValidationFromCartFailedEvent(
        Long userId,
        List<Map.Entry<Long, Integer>> sortedEntries,  // productId, quantity
        String failureReason
) {
    public static ValidationFromCartFailedEvent of(
            Long userId,
            List<Map.Entry<Long, Integer>> sortedEntries,
            String failureReason
    ) {
        return new ValidationFromCartFailedEvent(userId, sortedEntries, failureReason);
    }
}
