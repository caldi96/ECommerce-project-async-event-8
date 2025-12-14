package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.RedisCouponMetadataService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.service.CouponDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateCouponUseCase {

    private final CouponDomainService couponDomainService;
    private final CouponFinderService couponFinderService;
    private final RedisCouponMetadataService redisCouponMetadataService;

    @Transactional
    public Coupon execute(Long id) {

        // 1. 쿠폰 ID 검증
        couponDomainService.validateId(id);

        // 2. 쿠폰 마스터 조회
        Coupon coupon = couponFinderService.getCoupon(id);

        // 3. 쿠폰 활성화
        coupon.activate();

        // 4. Redis에 캐싱된 메타데이터 활성 상태로 변경
        redisCouponMetadataService.updateIsActive(id, true);

        log.info("쿠폰 활성화 완료 (Redis 동기화 포함) - couponId: {}", id);

        // 5. 저장 후 반환
        return coupon;
    }
}
