package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.annotation.DistributedLock;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 재고 차감/복구 서비스
 * - 분산락을 사용한 재고 처리
 * - AOP 프록시가 정상 작동하도록 별도 서비스로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDeductionService {

    private final ProductFinderService productFinderService;

    /**
     * 분산락을 사용한 상품 재고 차감
     */
    @DistributedLock(
        key = "'product:stock:' + #productId",
        waitTime = 3L,
        leaseTime = 5L  // 재고 차감 + 판매량 증가
    )
    public void deductStockWithLock(Long productId, Integer quantity) {
        Product product = productFinderService.getProduct(productId);
        product.decreaseStock(quantity);
        product.increaseSoldCount(quantity);

        log.debug("상품 재고 차감 완료 - productId: {}, 남은재고: {}, 판매량: {}",
            productId, product.getStock(), product.getSoldCount());
    }

    /**
     * 분산락을 사용한 재고 복구
     */
    @DistributedLock(
        key = "'product:stock:' + #productId",
        waitTime = 3L,
        leaseTime = 5L  // 재고 복구 + 판매량 감소
    )
    public void recoverStockWithLock(Long productId, Integer quantity) {
        Product product = productFinderService.getProduct(productId);
        product.increaseStock(quantity);
        product.decreaseSoldCount(quantity);

        log.debug("상품 재고 복구 완료 - productId: {}, 현재재고: {}, 판매량: {}",
            productId, product.getStock(), product.getSoldCount());
    }
}