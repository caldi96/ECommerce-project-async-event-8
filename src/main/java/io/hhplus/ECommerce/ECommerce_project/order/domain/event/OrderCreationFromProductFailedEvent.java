package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import java.util.List;

/**
 * 주문 생성 실패 이벤트
 * - 주문 완료 트랜잭션 실패 시 발행
 * - 재고 복구를 비동기로 처리
 */
public record OrderCreationFromProductFailedEvent(
        Long userId,
        List<StockReservation> reservations,  // 복수 상품 지원
        String failureReason,
        boolean needsDbStockRecovery    // DB 재고도 복구해야 하는지 여부
) {
    /**
     * 단일 상품 주문 실패
     */
    public static OrderCreationFromProductFailedEvent of(
            Long userId,
            Long productId,
            Integer quantity,
            String failureReason
    ) {
        return new OrderCreationFromProductFailedEvent(
                userId,
                List.of(new StockReservation(productId, quantity)),
                failureReason,
                true
        );
    }

    /**
     * 장바구니 주문 실패
     */
    public static OrderCreationFromProductFailedEvent ofMultipleProducts(
            Long userId,
            List<StockReservation> reservations,
            String failureReason
    ) {
        return new OrderCreationFromProductFailedEvent(userId, reservations, failureReason, true);
    }

    /**
     * 재고 예약 정보
     */
    public record StockReservation(
            Long productId,
            Integer quantity
    ) {}
}
