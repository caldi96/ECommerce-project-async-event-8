package io.hhplus.ECommerce.ECommerce_project.coupon.application.listener;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.CouponIssueFailedEvent;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.CouponIssuedEvent;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.UserValidationEvent;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
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
public class UserValidationEventListener {

    private final UserFinderService userFinderService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleValidation(UserValidationEvent event) {
        log.info("유저 존재 확인 시작 - userId: {}", event.userId());

        try {
            // 1. 유저 조회
            User user = userFinderService.getUser(event.userId());

            log.info("유저 존재 확인 성공 - userId: {}", event.userId());

            // 2. 다음 단계: 쿠폰 발급 (DB 저장)
            applicationEventPublisher.publishEvent(
                    CouponIssuedEvent.of(event.userId(), event.couponId())
            );

        } catch (Exception e) {
            log.error("유저 존재 확인 실패 - userId: {}", event.userId(), e);
            applicationEventPublisher.publishEvent(
                    CouponIssueFailedEvent.of(event.userId(), event.couponId(), "USER_NOT_FOUND")
            );
        }
    }
}
