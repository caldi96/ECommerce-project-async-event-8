package io.hhplus.ECommerce.ECommerce_project.order.application.listener;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderItemFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.event.OrderCancelEvent;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointUsageHistoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 취소 원복 이벤트 리스너
 * - 주문 취소 시 재고, 쿠폰, 포인트 복구를 비동기로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelOrderEventListener {

    private final OrderFinderService orderFinderService;
    private final OrderItemFinderService orderItemFinderService;
    private final ProductFinderService productFinderService;
    private final RedisStockService redisStockService;
    private final CouponFinderService couponFinderService;
    private final UserCouponFinderService userCouponFinderService;
    private final PointFinderService pointFinderService;
    private final PointUsageHistoryFinderService pointUsageHistoryFinderService;
    private final UserFinderService userFinderService;

    /**
     * 주문 취소 이벤트 처리
     * - 트랜잭션 커밋 후 비동기로 재고, 쿠폰, 포인트 복구
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCancelOrder(OrderCancelEvent event) {
        log.info("주문 취소 보상 트랜잭션 시작: orderId={}, userId={}",
                event.orderId(), event.userId());

        try {
            // 주문 조회
            Orders order = orderFinderService.getOrder(event.orderId());

            // 1. 재고 복구
            compensateStock(order);

            // 2. 쿠폰 복구
            compensateCoupon(order);

            // 3. 포인트 복구
            compensatePoint(order, event.userId());

            log.info("주문 취소 보상 트랜잭션 완료: orderId={}", event.orderId());

        } catch (Exception e) {
            log.error("주문 취소 보상 트랜잭션 실패: orderId={}, error={}",
                    event.orderId(), e.getMessage(), e);

            // TODO: 보상 실패 시 처리 로직
            // - Dead Letter Queue에 저장
            // - 관리자 알림 발송
            // - 재시도 스케줄링
        }
    }

    /**
     * 재고 복구
     * - DB 재고 증가 (비관적 락)
     * - Redis 재고 증가 (원자적 연산)
     */
    private void compensateStock(Orders order) {
        try {
            log.debug("재고 복구 시작: orderId={}", order.getId());

            List<OrderItem> orderItems = orderItemFinderService.getOrderItems(order.getId());

            for (OrderItem orderItem : orderItems) {
                Long productId = orderItem.getProduct().getId();

                // DB 재고 복구 (비관적 락)
                Product product = productFinderService.getProductWithLock(productId);
                product.increaseStock(orderItem.getQuantity());

                // Redis 재고 복구 (원자적 증가)
                redisStockService.increaseStock(productId, orderItem.getQuantity());

                log.debug("재고 복구 완료: productId={}, quantity={}",
                        productId, orderItem.getQuantity());
            }

            log.info("재고 복구 완료: orderId={}, itemCount={}",
                    order.getId(), orderItems.size());

        } catch (Exception e) {
            log.error("재고 복구 실패: orderId={}, error={}",
                    order.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 쿠폰 복구
     * - 사용자 쿠폰의 usedCount 감소
     * - issuedQuantity는 복구하지 않음 (한번 발급되면 영구적)
     */
    private void compensateCoupon(Orders order) {
        if (order.getCoupon() == null) {
            log.debug("쿠폰 사용 없음: orderId={}", order.getId());
            return;
        }

        try {
            log.debug("쿠폰 복구 시작: orderId={}, couponId={}",
                    order.getId(), order.getCoupon().getId());

            // 사용자 쿠폰 조회 (비관적 락)
            UserCoupon userCoupon = userCouponFinderService.getUserCouponWithLock(
                    order.getUser(),
                    order.getCoupon()
            );

            // 쿠폰 정보 조회
            Coupon coupon = couponFinderService.getCoupon(order.getCoupon().getId());

            // 쿠폰 사용 취소 (usedCount 감소)
            userCoupon.cancelUse(coupon.getPerUserLimit());

            log.info("쿠폰 복구 완료: orderId={}, couponId={}",
                    order.getId(), order.getCoupon().getId());

        } catch (Exception e) {
            log.error("쿠폰 복구 실패: orderId={}, couponId={}, error={}",
                    order.getId(), order.getCoupon().getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 포인트 복구
     * - PointUsageHistory를 기반으로 사용한 포인트 복구
     * - 원본 포인트의 remainingAmount 복구
     * - User의 포인트 잔액 복구
     */
    private void compensatePoint(Orders order, Long userId) {
        try {
            log.debug("포인트 복구 시작: orderId={}, userId={}",
                    order.getId(), userId);

            // 포인트 사용 내역 조회
            List<PointUsageHistory> pointUsageHistories =
                    pointUsageHistoryFinderService.getPointUsageHistories(order.getId());

            if (pointUsageHistories.isEmpty()) {
                log.debug("포인트 사용 없음: orderId={}", order.getId());
                return;
            }

            BigDecimal totalRestoredPoint = BigDecimal.ZERO;

            // 각 포인트 사용 내역에 대해 복구
            for (PointUsageHistory history : pointUsageHistories) {
                // 원본 포인트 조회 (비관적 락)
                Point originalPoint = pointFinderService.getPointWithLock(
                        history.getPoint().getId()
                );

                // 포인트 remainingAmount 복구
                originalPoint.restoreUsedAmount(history.getUsedAmount());

                // PointUsageHistory 취소 처리
                history.cancel();

                // 복구할 총 포인트 금액 누적
                totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());

                log.debug("포인트 복구: pointId={}, restoredAmount={}",
                        originalPoint.getId(), history.getUsedAmount());
            }

            // User의 포인트 잔액 복구
            if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
                User lockedUser = userFinderService.getUserWithLock(userId);
                lockedUser.refundPoint(totalRestoredPoint);

                log.info("포인트 복구 완료: orderId={}, userId={}, totalRestoredPoint={}",
                        order.getId(), userId, totalRestoredPoint);
            }

        } catch (Exception e) {
            log.error("포인트 복구 실패: orderId={}, userId={}, error={}",
                    order.getId(), userId, e.getMessage(), e);
            throw e;
        }
    }
}
