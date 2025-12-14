package io.hhplus.ECommerce.ECommerce_project.order.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderResponse(
    Long orderId,
    Long userId,
    BigDecimal totalAmount,
    BigDecimal shippingFee,
    BigDecimal discountAmount,
    BigDecimal pointAmount,
    BigDecimal finalAmount,
    OrderStatus orderStatus,
    LocalDateTime orderedAt,
    List<OrderItemResponse> orderItems,
    String message  // 주문 처리 메시지
) {
    public static CreateOrderResponse from(Orders order, List<OrderItem> orderItems) {
        return new CreateOrderResponse(
            order.getId(),
            order.getUser().getId(),
            order.getTotalAmount(),
            order.getShippingFee(),
            order.getDiscountAmount(),
            order.getPointAmount(),
            order.getFinalAmount(),
            order.getStatus(),
            order.getCreatedAt(),
            orderItems.stream()
                .map(OrderItemResponse::from)
                .toList(),
            "주문이 완료되었습니다."
        );
    }

    /**
     * 주문 접수 응답 (비동기 처리 중)
     * - Redis 재고 차감 후 즉시 반환
     * - 실제 주문 처리는 비동기로 진행
     */
    public static CreateOrderResponse accepted(Long userId, Long productId, Integer quantity) {
        return new CreateOrderResponse(
                null,  // orderId - 아직 생성 안됨
                userId,
                null,  // totalAmount - 계산 중
                null,  // shippingFee - 계산 중
                null,  // discountAmount - 계산 중
                null,  // pointAmount - 계산 중
                null,  // finalAmount - 계산 중
                OrderStatus.PENDING,  // 주문 중 상태
                LocalDateTime.now(),
                List.of(),  // 빈 주문 아이템 리스트
                "주문이 접수되었습니다. 처리가 완료되면 알림을 보내드립니다."
        );
    }

    /**
     * 장바구니 주문 접수 응답 (비동기 처리 중)
     */
    public static CreateOrderResponse acceptedForCart(Long userId, int cartItemCount) {
        return new CreateOrderResponse(
                null,
                userId,
                null,
                null,
                null,
                null,
                null,
                OrderStatus.PENDING,
                LocalDateTime.now(),
                List.of(),
                cartItemCount + "개 장바구니 아이템 주문이 접수되었습니다. 처리가 완료되면 알림을 보내드립니다."
        );
    }

    public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
    ) {
        public static OrderItemResponse from(OrderItem orderItem) {
            return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getSubTotal()
            );
        }
    }
}
