package io.hhplus.ECommerce.ECommerce_project.order.application.command;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderFromCartCommand(
        Long userId,
        List<Long> cartItemIds,
        Long couponId,           // 사용할 쿠폰 ID (선택, null 가능)
        BigDecimal pointAmount   // 사용할 포인트 (선택, null 가능)
) {
    public record CartItemCommand(
            Long cartItemId,  // 장바구니 아이템 ID
            Long productId,
            Integer quantity
    ) {}
}
