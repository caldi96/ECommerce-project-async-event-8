package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CancelOrderCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderItemFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderItemStatus;
import io.hhplus.ECommerce.ECommerce_project.order.domain.event.OrderCancelEvent;
import io.hhplus.ECommerce.ECommerce_project.order.domain.service.OrderDomainService;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderDomainService orderDomainService;
    private final OrderFinderService orderFinderService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final OrderItemFinderService orderItemFinderService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void execute(CancelOrderCommand command) {

        // 1. ID 검증
        orderDomainService.validateId(command.orderId());
        userDomainService.validateId(command.userId());

        // 2. 유저 존재 유무 확인
        User user = userFinderService.getUser(command.userId());

        // 3. 주문 조회
        Orders order = orderFinderService.getOrderWithLock(command.orderId());

        // 4. 주문 소유자 확인
        orderDomainService.validateOrderOwner(order, user);

        // 5. 주문 취소 가능 여부 확인 (PENDING, PAID, PAYMENT_FAILED 상태만 취소 가능)
        orderDomainService.validateCancelable(order);

        // 6. 주문 아이템 조회
        List<OrderItem> orderItems = orderItemFinderService.getOrderItems(command.orderId());

        // 7. 주문 아이템 상태 변경
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getStatus() == OrderItemStatus.ORDER_PENDING) {
                orderItem.cancel();
            } else if (orderItem.getStatus() == OrderItemStatus.ORDER_COMPLETED) {
                orderItem.cancelAfterComplete();
            }
        }

        // 8. 주문 상태 변경
        if (order.isPending()) {
            order.cancel();  // PENDING -> CANCELED (결제 전 주문 취소)
        } else if (order.isPaid()) {
            order.cancelAfterPaid();  // PAID -> CANCELED (결제 후 환불)
        } else if (order.isPaymentFailed()) {
            order.cancel();  // PAYMENT_FAILED -> CANCELED (결제 실패 후 취소)
        }

        // 9. 주문 취소 이벤트 발행 (비동기로 재고, 쿠폰, 포인트 복구)
        applicationEventPublisher.publishEvent(
                OrderCancelEvent.of(order.getId(), command.userId())
        );
    }
}