package io.hhplus.ECommerce.ECommerce_project.coupon.application.listener;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.CouponIssueFailedEvent;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.CouponValidationEvent;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.UserValidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponValidationEventListener {

    private final CouponFinderService couponFinderService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleValidation(CouponValidationEvent event) {
        log.info("쿠폰 유효성 검증 시작 - userId: {}, couponId: {}", event.userId(), event.couponId());

        try {
            // 1. 쿠폰 조회
            Coupon coupon = couponFinderService.getCoupon(event.couponId());

            // 2. 유효성 검증
            coupon.validateAvailability();

            log.info("쿠폰 유효성 검증 성공 - couponId: {}", event.couponId());

            // 3. 다음 단계: 유저 검증
            applicationEventPublisher.publishEvent(
                    UserValidationEvent.of(event.userId(), event.couponId())
            );
        } catch (Exception e) {
            log.error("쿠폰 유효성 검증 실패 - couponId: {}", event.couponId(), e);
            applicationEventPublisher.publishEvent(
                    CouponIssueFailedEvent.of(event.userId(), event.couponId(), "INVALID_COUPON")
            );
        }
    }
}
