package io.hhplus.ECommerce.ECommerce_project.order.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.annotation.DistributedLock;
import io.hhplus.ECommerce.ECommerce_project.order.domain.event.OrderCreationFromProductFailedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 복구 서비스
 * - 주문 생성 실패 시 재고 복구 처리
 * - Self-Invocation 문제 해결을 위해 별도 서비스로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRecoveryService {

    private final RedisStockService redisStockService;
    private final ProductFinderService productFinderService;

    /**
     * 개별 상품의 재고 복구 처리
     * - 분산락을 적용하여 동시성 제어
     * - 별도 트랜잭션으로 실행 (REQUIRES_NEW)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @DistributedLock(
            key = "'product:stock:' + #reservation.productId()",
            waitTime = 3L,
            leaseTime = 5L
    )
    public void recoverStockForProduct(boolean needsDbStockRecovery,
                                       OrderCreationFromProductFailedEvent.StockReservation reservation) {
        Long productId = reservation.productId();
        Integer quantity = reservation.quantity();

        if (needsDbStockRecovery) {
            // DB 재고 복구 (분산락으로 동시성 제어)
            Product product = productFinderService.getProduct(productId);
            product.increaseStock(quantity);
            product.decreaseSoldCount(quantity);

            log.info("DB 재고 복구 완료 - productId: {}, 복구수량: {}, 현재재고: {}",
                    productId, quantity, product.getStock());
        }

        // Redis 재고 복구
        redisStockService.increaseStock(productId, quantity);

        log.info("재고 복구 완료 - productId: {}, quantity: {}", productId, quantity);
    }
}