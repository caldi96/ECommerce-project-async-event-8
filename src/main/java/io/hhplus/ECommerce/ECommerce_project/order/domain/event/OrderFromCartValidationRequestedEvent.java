package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;

import java.util.List;
import java.util.Map;

/**
 * 장바구니 주문 검증 요청 이벤트
 * - Redis 재고 차감 후 발행
 * - sortedEntries: 상품별 주문 수량 (productId 오름차순 정렬, 데드락 방지)
 */
public record OrderFromCartValidationRequestedEvent(
        CreateOrderFromCartCommand command,
        List<Map.Entry<Long, Integer>> sortedEntries
) {
    public static OrderFromCartValidationRequestedEvent of(
            CreateOrderFromCartCommand command,
            List<Map.Entry<Long, Integer>> sortedEntries
    ) {
        return new OrderFromCartValidationRequestedEvent(command, sortedEntries);
    }
}
