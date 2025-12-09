package io.hhplus.ECommerce.ECommerce_project.payment.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePaymentResponse(
    Long paymentId,
    Long orderId,
    BigDecimal paymentAmount,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    LocalDateTime paidAt,
    OrderStatus orderStatus,
    String message
) {
    public static CreatePaymentResponse from(Payment payment, Orders order) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getCompletedAt(),
                order.getStatus(),
                payment.getPaymentStatus().getResponseMessage()
        );
    }

    public static CreatePaymentResponse failed(Payment payment, Orders order) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getFailedAt(),
                order.getStatus(),
                payment.getPaymentStatus().getResponseMessage()
        );
    }
}