package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.IssueCouponUseCase;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 동시성 통합 테스트 (JPA 기반)
 *
 * 시나리오:
 * - 제한된 쿠폰을 여러 사용자가 동시에 발급받는 경우
 * - 같은 사용자가 쿠폰을 동시에 여러 번 사용하는 경우
 * - 발급받은 쿠폰을 사용하여 주문하는 경우
 */
@SpringBootTest
@ActiveProfiles("integration")
public class CouponConcurrencyTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product testProduct;
    private Category testCategory;
    private Coupon limitedCoupon;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 카테고리 생성
        testCategory = Category.createCategory("테스트카테고리", 1);
        testCategory = categoryRepository.save(testCategory);

        // 테스트용 상품 생성 (충분한 재고)
        testProduct = Product.createProduct(
                testCategory,
                "쿠폰 테스트 상품",
                "동시성 테스트용",
                BigDecimal.valueOf(50000),
                1000,  // 충분한 재고
                1,
                100
        );
        testProduct = productRepository.save(testProduct);

        // 총 발급 횟수가 제한된 쿠폰 생성 (10개만 발급 가능)
        limitedCoupon = Coupon.createCoupon(
                "제한 쿠폰",
                "LIMITED10",
                DiscountType.FIXED,
                BigDecimal.valueOf(5000),
                null,
                BigDecimal.valueOf(30000),
                10,  // 총 10개만 발급 가능
                1,   // 사용자당 1번만
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        limitedCoupon = couponRepository.save(limitedCoupon);
    }

    @Test
    @DisplayName("제한된 쿠폰을 여러 사용자가 동시에 발급받을 때 정확히 제한 횟수만큼만 성공해야 한다")
    void testConcurrentCouponIssuanceWithLimit() throws InterruptedException {
        // Given
        int userCount = 20;  // 20명이 동시에 쿠폰 발급 시도
        int couponLimit = 10;  // 쿠폰은 10명에게만 발급 가능

        // 미리 사용자들 생성
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "coupon_user_" + i,
                    "password",
                    BigDecimal.ZERO,
                    null,
                    null
            );
            users[i] = userRepository.save(users[i]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20명이 동시에 쿠폰 발급 시도
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    IssueCouponCommand command = new IssueCouponCommand(
                            users[userIndex].getId(),
                            limitedCoupon.getId()
                    );
                    issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 1. 쿠폰 제한 횟수(10)만큼만 발급 성공해야 함
        assertThat(successCount.get()).isEqualTo(couponLimit);

        // 2. 나머지 10명은 발급 실패해야 함
        assertThat(failCount.get()).isEqualTo(userCount - couponLimit);

        // 3. 실제 발급된 UserCoupon 개수 확인
        long issuedCount = userCouponRepository.findAll().stream()
                .filter(uc -> uc.getCoupon().getId().equals(limitedCoupon.getId()))
                .count();
        assertThat(issuedCount).isEqualTo(couponLimit);

        // 4. Coupon의 issuedQuantity 확인
        Coupon finalCoupon = couponRepository.findById(limitedCoupon.getId()).orElseThrow();
        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(couponLimit);
    }

    @Test
    @DisplayName("같은 사용자가 동시에 같은 쿠폰을 여러 번 발급 시도할 때 1번만 성공해야 한다")
    void testSameUserConcurrentDuplicateIssuance() throws InterruptedException {
        // Given
        User user = new User("duplicate_user", "password", BigDecimal.ZERO, null, null);
        user = userRepository.save(user);
        final Long userId = user.getId();

        int attemptCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 같은 사용자가 동시에 5번 발급 시도
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    IssueCouponCommand command = new IssueCouponCommand(
                            userId,
                            limitedCoupon.getId()
                    );
                    issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 같은 사용자는 1번만 발급받아야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        // UserCoupon도 1개만 생성되어야 함
        UserCoupon userCoupon = userCouponRepository
                .findByUser_IdAndCoupon_Id(userId, limitedCoupon.getId())
                .orElseThrow();
        assertThat(userCoupon).isNotNull();
    }

    @Test
    @DisplayName("동일 사용자가 쿠폰을 동시에 여러 번 사용 시도할 때 1번만 성공해야 한다")
    void testConcurrentCouponUsageBySameUser() throws InterruptedException {
        // Given
        // 사용자당 1번만 사용 가능한 쿠폰
        Coupon usageLimitCoupon = Coupon.createCoupon(
                "사용자당 1번 쿠폰",
                "PERUSER1",
                DiscountType.FIXED,
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.valueOf(20000),
                100,  // 총 100번 사용 가능
                1,    // 사용자당 1번만
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        usageLimitCoupon = couponRepository.save(usageLimitCoupon);
        final Long couponId = usageLimitCoupon.getId();

        User user = new User("usage_user", "password", BigDecimal.ZERO, null, null);
        user = userRepository.save(user);
        final Long userId = user.getId();

        // 사용자에게 쿠폰 발급
        IssueCouponCommand issueCommand = new IssueCouponCommand(userId, couponId);
        issueCouponUseCase.execute(issueCommand);

        // 동시에 5번 주문 시도 (모두 같은 쿠폰 사용)
        int orderAttempts = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(orderAttempts);
        CountDownLatch latch = new CountDownLatch(orderAttempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < orderAttempts; i++) {
            executorService.submit(() -> {
                try {
                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            userId,
                            testProduct.getId(),
                            1,
                            null,
                            couponId
                    );
                    createOrderFromProductUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 사용자당 1번만 사용 가능하므로 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        // UserCoupon 사용 횟수 확인
        UserCoupon finalUserCoupon = userCouponRepository
                .findByUser_IdAndCoupon_Id(userId, couponId)
                .orElseThrow();
        assertThat(finalUserCoupon.getUsedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 사용자가 각자의 쿠폰을 동시에 사용할 때 모두 성공해야 한다")
    void testConcurrentCouponUsageByMultipleUsers() throws InterruptedException {
        // Given
        // 충분한 사용 횟수를 가진 쿠폰
        Coupon abundantCoupon = Coupon.createCoupon(
                "넉넉한 쿠폰",
                "ABUNDANT",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                100,  // 총 100번 사용 가능
                3,    // 사용자당 3번
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        abundantCoupon = couponRepository.save(abundantCoupon);
        final Long couponId = abundantCoupon.getId();

        int userCount = 20;

        // 미리 사용자들 생성 및 쿠폰 발급
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "multi_user_" + i,
                    "password",
                    BigDecimal.ZERO,
                    null,
                    null
            );
            users[i] = userRepository.save(users[i]);

            // 쿠폰 발급
            IssueCouponCommand issueCommand = new IssueCouponCommand(users[i].getId(), couponId);
            issueCouponUseCase.execute(issueCommand);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20명이 동시에 쿠폰 사용하여 주문
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            users[userIndex].getId(),
                            testProduct.getId(),
                            1,
                            null,
                            couponId
                    );
                    createOrderFromProductUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 쿠폰이 충분하므로 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 사용자의 쿠폰 사용 횟수 확인
        for (User user : users) {
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_Id(user.getId(), couponId)
                    .orElseThrow();
            assertThat(userCoupon.getUsedCount()).isEqualTo(1);
        }
    }
}