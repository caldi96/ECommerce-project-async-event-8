package io.hhplus.ECommerce.ECommerce_project.order.domain.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
public class OrderDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new OrderException(ErrorCode.ORDER_ID_INVALID);
        }
    }

    /**
     * 주문 소유자 검증
     */
    public void validateOrderOwner(Orders order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new OrderException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    /**
     * 주문 취소 가능 상태인지 확인
     */
    public void validateCancelable(Orders order) {
        if (!order.canCancel() && !order.canCancelAfterPaid()) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_CANCEL,
                    " 현재 상태: " + order.getStatus());
        }
    }
}
