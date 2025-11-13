package io.hhplus.ECommerce.ECommerce_project.payment.application;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentStatus;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.infrastructure.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CreatePaymentUseCase.class) // 테스트할 UseCase 주입
@Transactional
public class CreatePaymentUseCaseTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User testUser;
    private Product testProduct;
    private Orders testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        // 1. 사용자 생성
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setPointBalance(BigDecimal.valueOf(1000));
        userRepository.save(testUser);

        // 2. 상품 생성
        testProduct = new Product();
        testProduct.setName("테스트 상품");
        testProduct.setPrice(BigDecimal.valueOf(500));
        testProduct.setStock(100);
        testProduct.setSoldCount(0);
        productRepository.save(testProduct);

        // 3. 주문 생성
        testOrder = new Orders();
        testOrder.setUser(testUser);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setFinalAmount(BigDecimal.valueOf(500));
        orderRepository.save(testOrder);

        // 4. 주문 아이템 생성
        testOrderItem = new OrderItem();
        testOrderItem.setOrders(testOrder);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(1);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(500));
        orderItemRepository.save(testOrderItem);
    }

    @Test
    @DisplayName("정상 결제 처리 테스트")
    void testCreatePayment() {
        // 결제 커맨드 생성
        CreatePaymentCommand command = new CreatePaymentCommand(
                testOrder.getId(),
                PaymentMethod.CARD // 결제 수단
        );

        // 결제 실행
        var response = createPaymentUseCase.execute(command);

        // 결과 검증
        assertThat(response).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);

        // DB 확인
        List<Payment> payments = paymentRepository.findByOrder_Id(testOrder.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("결제 실패 시 보상 트랜잭션 테스트")
    void testPaymentFailureRollback() {
        // 결제를 실패하도록 외부 API 시뮬레이션 변경 가능
        CreatePaymentCommand command = new CreatePaymentCommand(
                testOrder.getId(),
                PaymentMethod.CARD // 실패 유도
        );

        try {
            createPaymentUseCase.execute(command);
        } catch (Exception e) {
            // 예외 발생 예상
        }

        // 주문 상태가 결제 실패로 변경되었는지 확인
        Orders orderAfter = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);

        // 상품 재고가 원래대로 복구되었는지 확인
        Product productAfter = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productAfter.getStock()).isEqualTo(100);
        assertThat(productAfter.getSoldCount()).isEqualTo(0);
    }
}
