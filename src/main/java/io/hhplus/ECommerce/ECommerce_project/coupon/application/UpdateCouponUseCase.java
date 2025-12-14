package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.UpdateCouponCommand;
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
public class UpdateCouponUseCase {

    private final CouponDomainService couponDomainService;
    private final CouponFinderService couponFinderService;
    private final RedisCouponMetadataService redisCouponMetadataService;

    @Transactional
    public Coupon execute(UpdateCouponCommand command) {

        // 1. 쿠폰 ID 검증
        couponDomainService.validateId(command.id());

        // 2. 쿠폰 조회
        Coupon coupon = couponFinderService.getCoupon(command.id());

        // 3. 쿠폰 정보 수정
        coupon.updateName(command.name());
        coupon.updateCode(command.code());
        coupon.updateDiscountInfo(
                command.discountType(),
                command.discountValue(),
                command.maxDiscountAmount()
        );
        coupon.updateMinOrderAmount(command.minOrderAmount());
        coupon.updateTotalQuantity(command.totalQuantity());
        coupon.updatePerUserLimit(command.perUserLimit());
        coupon.updateDateRange(command.startDate(), command.endDate());

        // 4. Redis 메타데이터 업데이트 (동기화)
        redisCouponMetadataService.saveCouponMetadata(
                coupon.getId(),
                coupon.getTotalQuantity(),
                coupon.isActive(),
                coupon.getStartDate(),
                coupon.getEndDate()
        );

        log.info("쿠폰 수정 완료 (Redis 동기화 포함) - couponId: {}", coupon.getId());

        // 5. 수정 후 반환
        return coupon;
    }
}