package io.hhplus.ECommerce.ECommerce_project.order.application.listener;

import io.hhplus.ECommerce.ECommerce_project.common.annotation.DistributedLock;
import io.hhplus.ECommerce.ECommerce_project.order.domain.event.OrderCreationFromProductFailedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
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
public class OrderCreationFromProductFailedEventListener {

    private final RedisStockService redisStockService;
    private final ProductFinderService productFinderService;

    /**
     * 주문 생성 실패 이벤트 처리
     * - DB 재고 및 Redis 재고를 비동기로 복구
     */
    @Async  // TODO: Kafka 도입 시 메시지 컨슈머로 변경 예정
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @DistributedLock(  // TODO: Kafka 도입 후 @DistributedLock을 Kafka Consumer에서 처리
            key = "'product:stock:' + #event.productId()",
            waitTime = 3L,
            leaseTime = 5L  // 재고 복구 + 판매량 감소
    )
    public void handleOrderCreationFailed(OrderCreationFromProductFailedEvent event) {
        log.info("주문 생성 실패 재고 복구 시작 - userId: {}, productId: {}, quantity: {}, reason: {}",
                event.userId(), event.productId(), event.quantity(), event.failureReason());

        try {
            if (event.needsDbStockRecovery()) {
                // DB 재고 복구 (분산락으로 동시성 제어)
                Product product = productFinderService.getProduct(event.productId());
                product.increaseStock(event.quantity());
                product.decreaseSoldCount(event.quantity());

                log.info("DB 재고 복구 완료 - productId: {}, 복구수량: {}, 현재재고: {}",
                        event.productId(), event.quantity(), product.getStock());
            }

            // Redis 재고 복구
            redisStockService.increaseStock(event.productId(), event.quantity());

            log.info("주문 생성 실패 재고 복구 완료 - userId: {}, productId: {}, quantity: {}",
                    event.userId(), event.productId(), event.quantity());

        } catch (Exception e) {
            log.error("주문 생성 실패 재고 복구 실패 - userId: {}, productId: {}, quantity: {}, error: {}",
                    event.userId(), event.productId(), event.quantity(), e.getMessage(), e);

            // TODO: 재고 복구 실패 시 처리 로직
            // - Dead Letter Queue에 저장
            // - 관리자 알림 발송
            // - 재시도 스케줄링
            // - Kafka 도입 후 DLQ 토픽으로 전송
        }
    }
}
