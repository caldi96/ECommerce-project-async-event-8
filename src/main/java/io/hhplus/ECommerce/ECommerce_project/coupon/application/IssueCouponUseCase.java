package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.annotation.DistributedLock;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.RedisCouponService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.event.CouponIssuedEvent;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.service.CouponDomainService;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final UserDomainService userDomainService;
    private final CouponDomainService couponDomainService;
    private final UserFinderService userFinderService;
    private final CouponFinderService couponFinderService;
    private final RedisCouponService redisCouponService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Redis 분산 락 + 비동기 이벤트 발행
     * - Redis 발급 성공 후 즉시 반환
     * - DB 저장은 비동기 이벤트로 처리
     */
    @DistributedLock(
            key = "'coupon:issue:' + #command.couponId()",
            waitTime = 2L,
            leaseTime = 5L
    )
    public void execute(IssueCouponCommand command) {

        userDomainService.validateId(command.userId());
        couponDomainService.validateId(command.couponId());

        // 1. 유저 존재 유무 확인 및 조회
        User user = userFinderService.getUser(command.userId());

        // 3. 쿠폰 조회 (비관적 락 제거 - 분산 락으로 동시성 제어)
        Coupon coupon = couponFinderService.getCoupon(command.couponId());

        // 4. 쿠폰 유효성 검증 (활성화 상태, 사용 기간)
        coupon.validateAvailability();

        // 5. Redis Lua Script로 선착순 발급 시도
        boolean issued = redisCouponService.tryIssueCoupon(
                command.couponId(),
                command.userId(),
                coupon.getTotalQuantity()
        );

        if (!issued) {
            throw new CouponException(ErrorCode.COUPON_ALL_ISSUED);
        }

        // 6. 비동기 이벤트 발행 (DB 저장은 리스너에서 처리)
        applicationEventPublisher.publishEvent(
                CouponIssuedEvent.of(command.userId(), command.couponId())
        );
    }
}