package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 동시성 통합 테스트 (JPA 기반)
 *
 * 시나리오:
 * - 여러 사용자가 동시에 같은 상품을 주문할 때 재고 감소
 * - 재고 부족 상황에서 동시 주문 처리
 * - 재고가 정확히 주문 수량만큼만 감소하는지 확인
 */
@SpringBootTest
@ActiveProfiles("integration")
public class StockConcurrencyTest {

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (외래 키 제약조건 고려)
        orderItemRepository.deleteAll();  // OrderItem -> Order, Product 참조
        orderRepository.deleteAll();      // Order -> User, Product 참조
        productRepository.deleteAll();    // Product -> Category 참조
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // 테스트 카테고리 생성
        testCategory = Category.createCategory("테스트카테고리", 1);
        testCategory = categoryRepository.save(testCategory);

        // 테스트용 상품 생성 (재고 100개)
        testProduct = Product.createProduct(
                testCategory,
                "동시성 테스트 상품",
                "재고 동시성 테스트용",
                BigDecimal.valueOf(10000),
                100,  // 재고 100개
                1,
                10
        );
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 같은 상품을 주문할 때 재고가 정확히 감소해야 한다")
    void testConcurrentStockDecrease() throws InterruptedException {
        // Given
        int userCount = 50;  // 50명이 동시에 주문
        int orderQuantityPerUser = 1;  // 각자 1개씩 주문

        // 미리 사용자들 생성
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "stock_user_" + i,
                    "password",
                    BigDecimal.ZERO,
                    null,
                    null
            );
            users[i] = userRepository.save(users[i]);
        }

        int initialStock = testProduct.getStock();
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 50명이 동시에 1개씩 주문 (총 50개 주문)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                                users[userIndex].getId(),
                                testProduct.getId(),
                                orderQuantityPerUser,
                                null,
                                null
                        );
                        return createOrderFromProductUseCase.execute(command);
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Order failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 모두 성공해야 함 (재고 100개, 주문 50개)
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 재고가 정확히 50개 감소했는지 확인
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - userCount * orderQuantityPerUser);
        assertThat(updatedProduct.getSoldCount()).isEqualTo(userCount * orderQuantityPerUser);
    }

    @Test
    @DisplayName("재고가 부족한 상황에서 동시에 주문할 때 일부만 성공해야 한다")
    void testConcurrentStockShortage() throws InterruptedException {
        // Given
        // 재고를 20개로 설정
        testProduct.updateStock(20);
        testProduct = productRepository.save(testProduct);

        int userCount = 50;  // 50명이 동시에 주문 시도
        int orderQuantityPerUser = 1;  // 각자 1개씩

        // 미리 사용자들 생성
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "shortage_user_" + i,
                    "password",
                    BigDecimal.ZERO,
                    null,
                    null
            );
            users[i] = userRepository.save(users[i]);
        }

        int initialStock = testProduct.getStock();
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 50명이 동시에 1개씩 주문 시도 (재고는 20개만)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                                users[userIndex].getId(),
                                testProduct.getId(),
                                orderQuantityPerUser,
                                null,
                                null
                        );
                        return createOrderFromProductUseCase.execute(command);
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Order failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 재고(20개)만큼만 주문 성공해야 함
        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(userCount - initialStock);

        // 재고가 0이 되어야 함
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(0);
        assertThat(updatedProduct.getSoldCount()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("동시에 여러 개씩 주문할 때 재고가 정확히 감소해야 한다")
    void testConcurrentMultipleQuantityOrder() throws InterruptedException {
        // Given
        int userCount = 20;  // 20명이 동시에 주문
        int orderQuantityPerUser = 3;  // 각자 3개씩 주문 (총 60개)

        // 미리 사용자들 생성
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "multi_order_user_" + i,
                    "password",
                    BigDecimal.ZERO,
                    null,
                    null
            );
            users[i] = userRepository.save(users[i]);
        }

        int initialStock = testProduct.getStock();
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20명이 동시에 3개씩 주문 (총 60개)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                                users[userIndex].getId(),
                                testProduct.getId(),
                                orderQuantityPerUser,
                                null,
                                null
                        );
                        return createOrderFromProductUseCase.execute(command);
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Order failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 모두 성공해야 함 (재고 100개, 주문 60개)
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 재고가 정확히 60개 감소했는지 확인
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        int expectedRemainingStock = initialStock - (userCount * orderQuantityPerUser);
        assertThat(updatedProduct.getStock()).isEqualTo(expectedRemainingStock);
        assertThat(updatedProduct.getSoldCount()).isEqualTo(userCount * orderQuantityPerUser);
    }

    @Test
    @DisplayName("재고가 정확히 주문 수량과 같을 때 모두 성공해야 한다")
    void testConcurrentExactStockMatch() throws InterruptedException {
        // Given
        // 재고를 정확히 30개로 설정
        testProduct.updateStock(30);
        testProduct = productRepository.save(testProduct);

        int userCount = 30;  // 30명이 동시에 주문
        int orderQuantityPerUser = 1;  // 각자 1개씩

        // 미리 사용자들 생성
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "exact_user_" + i,
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

        // When: 30명이 동시에 1개씩 주문 (재고도 정확히 30개)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                                users[userIndex].getId(),
                                testProduct.getId(),
                                orderQuantityPerUser,
                                null,
                                null
                        );
                        return createOrderFromProductUseCase.execute(command);
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Order failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
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

        // 재고가 정확히 0이 되어야 함
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(0);
        assertThat(updatedProduct.getSoldCount()).isEqualTo(30);
    }
}