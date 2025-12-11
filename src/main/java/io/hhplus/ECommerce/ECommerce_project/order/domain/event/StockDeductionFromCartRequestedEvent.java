package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromCartData;

/**
 * 장바구니 DB 재고 차감 요청 이벤트
 * - 검증 완료 후 발행
 */
public record StockDeductionFromCartRequestedEvent(
        CreateOrderFromCartCommand command,
        ValidatedOrderFromCartData validatedOrderFromCartData
) {
    public static StockDeductionFromCartRequestedEvent of(
            CreateOrderFromCartCommand command,
            ValidatedOrderFromCartData validatedOrderFromCartData
    ) {
        return new StockDeductionFromCartRequestedEvent(command, validatedOrderFromCartData);
    }
}
