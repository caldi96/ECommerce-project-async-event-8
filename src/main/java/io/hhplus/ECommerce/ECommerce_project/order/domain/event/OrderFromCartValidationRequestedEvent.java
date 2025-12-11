package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;

/**
 * 장바구니 주문 검증 요청 이벤트
 * - Redis 재고 차감 후 발행
 */
public record OrderFromCartValidationRequestedEvent(
        CreateOrderFromCartCommand command
) {
    public static OrderFromCartValidationRequestedEvent of(CreateOrderFromCartCommand command) {
        return new OrderFromCartValidationRequestedEvent(command);
    }
}
