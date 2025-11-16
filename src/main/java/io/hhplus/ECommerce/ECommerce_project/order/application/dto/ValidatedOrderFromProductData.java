package io.hhplus.ECommerce.ECommerce_project.order.application.dto;

import java.math.BigDecimal;

public record ValidatedOrderFromProductData(
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount
) {}
