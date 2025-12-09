package io.hhplus.ECommerce.ECommerce_project.order.application.listener;

import io.hhplus.ECommerce.ECommerce_project.order.domain.event.OrderCreationFailedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 생성 실패 이벤트 리스너
 * - 주문 완료 트랜잭션 실패 시 재고 복구를 비동기로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreationFailedEventListener {

    private final StockService stockService;

    /**
     * 주문 생성 실패 이벤트 처리
     * - 예약된 재고를 비동기로 복구
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreationFailed(OrderCreationFailedEvent event) {
        log.info("주문 생성 실패 재고 복구 시작: userId={}, stockCount={}, reason={}",
                event.userId(), event.stockReservations().size(), event.failureReason());

        try {
            // 모든 예약된 재고 복구
            for (OrderCreationFailedEvent.StockReservation reservation : event.stockReservations()) {
                stockService.compensateStock(
                        reservation.productId(),
                        reservation.quantity()
                );
                // ↓ StockService 내부에서 자동 처리
                // - Redis 재고 복구
                // - StockIncreasedEvent 발행
                // - StockEventListener가 DB 처리

                log.debug("재고 복구 완료: productId={}, quantity={}",
                        reservation.productId(), reservation.quantity());
            }

            log.info("주문 생성 실패 재고 복구 완료: userId={}, stockCount={}",
                    event.userId(), event.stockReservations().size());

        } catch (Exception e) {
            log.error("주문 생성 실패 재고 복구 실패: userId={}, error={}",
                    event.userId(), e.getMessage(), e);

            // TODO: 재고 복구 실패 시 처리 로직
            // - Dead Letter Queue에 저장
            // - 관리자 알림 발송
            // - 재시도 스케줄링
        }
    }
}
