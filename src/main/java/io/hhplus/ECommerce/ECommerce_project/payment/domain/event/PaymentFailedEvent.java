package io.hhplus.ECommerce.ECommerce_project.payment.domain.event;

/**
 * 결제 실패 이벤트
 * - 결제 실패 시 발행
 * - 주문 생성 중 차감한 리소스(재고, 쿠폰, 포인트)를 복구하기 위한 보상 트랜잭션 트리거
 */
public record PaymentFailedEvent(
        Long orderId,
        Long userId,
        String failureReason
) {
    public static PaymentFailedEvent of(Long orderId, Long userId, String failureReason) {
        return new PaymentFailedEvent(orderId, userId, failureReason);
    }
}
