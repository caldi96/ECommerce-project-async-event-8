package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;

/**
 * 장바구니 주문 완료 이벤트
 * - 주문 생성 완료 후 발행
 * - 사용자에게 최종 주문 결과 알림 전송용
 */
public record OrderFromCartCompletedEvent(
        Long userId,
        CreateOrderResponse createOrderResponse
) {
    public static OrderFromCartCompletedEvent of(Long userId, CreateOrderResponse createOrderResponse) {
        return new OrderFromCartCompletedEvent(userId, createOrderResponse);
    }
}
