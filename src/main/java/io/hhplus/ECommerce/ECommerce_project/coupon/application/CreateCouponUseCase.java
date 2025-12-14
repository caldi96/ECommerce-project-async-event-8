package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.CreateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.RedisCouponMetadataService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final RedisCouponMetadataService redisCouponMetadataService;

    @Transactional
    public Coupon execute(CreateCouponCommand command) {
        // 1. 도메인 생성
        Coupon coupon = Coupon.createCoupon(
                command.name(),
                command.code(),
                command.discountType(),
                command.discountValue(),
                command.maxDiscountAmount(),
                command.minOrderAmount(),
                command.totalQuantity(),
                command.perUserLimit(),
                command.startDate(),
                command.endDate()
        );

        // 2. DB 저장
        Coupon saved = couponRepository.save(coupon);

        // 3. Redis에 메타데이터 캐싱
        redisCouponMetadataService.saveCouponMetadata(
                saved.getId(),
                saved.getTotalQuantity(),
                saved.isActive(),
                saved.getStartDate(),
                saved.getEndDate()
        );

        log.info("쿠폰 생성 완료 (Redis 캐싱 포함) - couponId: {}, name: {}", saved.getId(), saved.getName());

        return saved;
    }
}
