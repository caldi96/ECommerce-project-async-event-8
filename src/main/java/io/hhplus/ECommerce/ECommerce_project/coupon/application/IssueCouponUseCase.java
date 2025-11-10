package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    // (userId, couponId) 조합별 락 맵 (같은 사용자의 동일 쿠폰 중복 발급 방지)
    private final Map<String, Object> userCouponLockMap = new ConcurrentHashMap<>();

    /**
     * (userId, couponId) 조합별 락 객체 획득
     */
    private Object getUserCouponLock(Long userId, Long couponId) {
        String key = userId + "-" + couponId;
        return userCouponLockMap.computeIfAbsent(key, k -> new Object());
    }

    /**
     * 선착순 쿠폰 발급 (동시성 제어)
     * - 사용자당 1개만 발급
     * - (userId, couponId) 조합별 락을 통한 중복 발급 방지
     * - 쿠폰 ID별 락을 통한 선착순 수량 제어
     */
    @Transactional
    public UserCoupon execute(IssueCouponCommand command) {
        // 같은 사용자의 동일 쿠폰 동시 발급 요청을 순차 처리
        synchronized (getUserCouponLock(command.userId(), command.couponId())) {
            // 1. 쿠폰 조회
            Coupon coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 2. 쿠폰 유효성 검증 (활성화 상태, 사용 기간)
            coupon.validateAvailability();

            // 3. 이미 발급받았는지 확인 (사용자당 1개 제한)
            // 락 안에서 확인하므로 동시 요청 시 중복 발급 방지
            userCouponRepository.findByUserIdAndCouponId(command.userId(), command.couponId())
                    .ifPresent(uc -> {
                        throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
                    });

            // 4. 쿠폰 발급 수량 증가 (쿠폰 ID별 락을 통한 동시성 제어)
            // increaseIssuedQuantity() 내부에서 수량 검증 수행
            Coupon updatedCoupon = couponRepository.increaseIssuedQuantityWithLock(command.couponId());

            // 5. 수량 증가 실패 시 예외 처리
            if (updatedCoupon == null) {
                throw new CouponException(ErrorCode.COUPON_NOT_FOUND);
            }

            // 6. UserCoupon 생성 및 저장
            UserCoupon userCoupon = UserCoupon.issueCoupon(command.userId(), command.couponId());
            return userCouponRepository.save(userCoupon);
        }
    }
}