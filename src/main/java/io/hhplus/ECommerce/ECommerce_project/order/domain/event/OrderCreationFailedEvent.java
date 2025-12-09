package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import java.util.List;
import java.util.Map;

/**
 * 주문 생성 실패 이벤트
 * - 주문 완료 트랜잭션 실패 시 발행
 * - 재고 복구를 비동기로 처리
 */
public record OrderCreationFailedEvent(
        Long userId,
        List<StockReservation> stockReservations,
        String failureReason
) {
    /**
     * 단일 상품 주문 실패
     */
    public static OrderCreationFailedEvent ofSingleProduct(
            Long userId,
            Long productId,
            Integer quantity,
            String failureReason
    ) {
        return new OrderCreationFailedEvent(
                userId,
                List.of(new StockReservation(productId, quantity)),
                failureReason
        );
    }

    /**
     * 장바구니 주문 실패
     */
    public static OrderCreationFailedEvent ofMultipleProducts(
            Long userId,
            List<Map.Entry<Long, Integer>> sortedEntries,
            String failureReason
    ) {

        List<StockReservation> reservations = sortedEntries.stream()
                .map(entry -> new StockReservation(entry.getKey(), entry.getValue()))
                .toList();

        return new OrderCreationFailedEvent(userId, reservations, failureReason);
    }

    /**
     * 재고 예약 정보
     */
    public record StockReservation(
            Long productId,
            Integer quantity
    ) {}
}
