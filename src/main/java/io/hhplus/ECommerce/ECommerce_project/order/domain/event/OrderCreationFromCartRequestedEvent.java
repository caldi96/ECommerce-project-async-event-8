package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromCartData;

/**
 * 장바구니 주문 생성 요청 이벤트
 * - DB 재고 차감 완료 후 발행
 */
public record OrderCreationFromCartRequestedEvent(
        CreateOrderFromCartCommand command,
        ValidatedOrderFromCartData validatedOrderFromCartData
) {
    public static OrderCreationFromCartRequestedEvent of(
            CreateOrderFromCartCommand command,
            ValidatedOrderFromCartData validatedOrderFromCartData
    ) {
        return new OrderCreationFromCartRequestedEvent(command, validatedOrderFromCartData);
    }
}
