package io.hhplus.ECommerce.ECommerce_project.payment.domain.event;

import java.util.List;

/**
 * 결제 완료 이벤트
 * - 결제 완료 후 발행
 * - Redis 랭킹 업데이트를 위한 비동기 이벤트
 */

public record PaymentCompletedEvent(
        Long orderId,
        List<OrderItemInfo> orderItems
) {
    public record OrderItemInfo(
            Long productId,
            Integer quantity
    ) {}

    public static PaymentCompletedEvent of(Long orderId, List<OrderItemInfo> orderItems) {
        return new PaymentCompletedEvent(orderId, orderItems);
    }
}
