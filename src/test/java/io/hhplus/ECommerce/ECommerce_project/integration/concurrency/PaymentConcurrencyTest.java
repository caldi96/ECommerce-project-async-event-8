package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.payment.application.CreatePaymentUseCase;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentStatus;
import io.hhplus.ECommerce.ECommerce_project.payment.infrastructure.PaymentRepository;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제 동시성 통합 테스트 (JPA 기반)
 *
 * 시나리오:
 * - 같은 주문을 동시에 여러 번 결제 시도 (1번만 성공해야 함)
 * - 여러 주문을 동시에 결제 (모두 성공해야 함)
 * - 결제 후 주문 상태 변경 확인
 */
@SpringBootTest
@ActiveProfiles("integration")
public class PaymentConcurrencyTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 카테고리 생성
        testCategory = Category.createCategory("테스트카테고리", 1);
        testCategory = categoryRepository.save(testCategory);

        // 테스트용 상품 생성
        testProduct = Product.createProduct(
                testCategory,
                "결제 테스트 상품",
                "동시성 테스트용",
                BigDecimal.valueOf(10000),
                1000,  // 충분한 재고
                1,
                10
        );
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("같은 주문을 동시에 여러 번 결제 시도할 때 1번만 성공해야 한다")
    void testConcurrentPaymentForSameOrder() throws InterruptedException {
        // Given
        // 사용자 생성
        User user = new User("payment_user", "password", BigDecimal.ZERO, null, null);
        user = userRepository.save(user);

        // 주문 생성
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                user.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
        final Long orderId = orderResponse.orderId();

        int attemptCount = 5;  // 5번 동시 결제 시도
        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 같은 주문을 동시에 5번 결제 시도
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderId,
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        // 1번만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        // Payment가 1개만 생성되었는지 확인
        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // 주문 상태가 PAID로 변경되었는지 확인
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("여러 주문을 동시에 결제할 때 모두 성공해야 한다")
    void testConcurrentPaymentForMultipleOrders() throws InterruptedException {
        // Given
        int orderCount = 10;  // 10개의 주문 생성

        // 사용자들 생성 및 주문 생성
        User[] users = new User[orderCount];
        Long[] orderIds = new Long[orderCount];

        for (int i = 0; i < orderCount; i++) {
            users[i] = new User("multi_payment_user_" + i, "password", BigDecimal.ZERO, null, null);
            users[i] = userRepository.save(users[i]);

            CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                    users[i].getId(),
                    testProduct.getId(),
                    1,
                    null,
                    null
            );
            CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
            orderIds[i] = orderResponse.orderId();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 10개의 주문을 동시에 결제
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderIds[index],
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        // 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(orderCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 주문이 PAID 상태인지 확인
        for (Long orderId : orderIds) {
            Orders order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            // 각 주문마다 Payment가 1개씩 생성되었는지 확인
            List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Test
    @DisplayName("서로 다른 주문을 동시에 결제할 때 각각 독립적으로 처리되어야 한다")
    void testConcurrentPaymentIndependence() throws InterruptedException {
        // Given
        int userCount = 20;

        // 각 사용자별 주문 생성
        User[] users = new User[userCount];
        Long[] orderIds = new Long[userCount];

        for (int i = 0; i < userCount; i++) {
            users[i] = new User("independent_user_" + i, "password", BigDecimal.ZERO, null, null);
            users[i] = userRepository.save(users[i]);

            CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                    users[i].getId(),
                    testProduct.getId(),
                    1,
                    null,
                    null
            );
            CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
            orderIds[i] = orderResponse.orderId();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20개의 서로 다른 주문을 동시에 결제
        for (int i = 0; i < userCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderIds[index],
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        // 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 Payment가 정상적으로 생성되었는지 확인
        List<Payment> allPayments = paymentRepository.findAll();
        assertThat(allPayments).hasSize(userCount);

        // 모든 주문이 PAID 상태인지 확인
        for (Long orderId : orderIds) {
            Orders order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }
    }

    @Test
    @DisplayName("동일 사용자가 여러 주문을 동시에 결제할 때 모두 성공해야 한다")
    void testConcurrentPaymentBySameUser() throws InterruptedException {
        // Given
        // 사용자 1명 생성
        User user = new User("same_user_payment", "password", BigDecimal.ZERO, null, null);
        user = userRepository.save(user);
        final Long userId = user.getId();

        int orderCount = 5;
        Long[] orderIds = new Long[orderCount];

        // 같은 사용자의 주문 5개 생성
        for (int i = 0; i < orderCount; i++) {
            CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                    userId,
                    testProduct.getId(),
                    1,
                    null,
                    null
            );
            CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
            orderIds[i] = orderResponse.orderId();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 같은 사용자의 5개 주문을 동시에 결제
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderIds[index],
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        // 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(orderCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 주문이 PAID 상태인지 확인
        for (Long orderId : orderIds) {
            Orders order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            // 각 주문마다 Payment가 1개씩 생성되었는지 확인
            List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
            assertThat(payments).hasSize(1);
        }
    }
}