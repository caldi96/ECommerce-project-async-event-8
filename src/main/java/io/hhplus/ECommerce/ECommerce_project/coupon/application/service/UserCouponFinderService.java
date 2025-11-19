package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserCouponFinderService {

    private final UserCouponRepository userCouponRepository;

    /**
     * 유저 ID와 쿠폰 ID로 유저 쿠폰 조회
     */
    public Optional<UserCoupon> getUserCouponByUserIdAndCouponId(Long userId, Long couponId) {
        return userCouponRepository.findByUser_IdAndCoupon_Id(userId, couponId);
    }
}
