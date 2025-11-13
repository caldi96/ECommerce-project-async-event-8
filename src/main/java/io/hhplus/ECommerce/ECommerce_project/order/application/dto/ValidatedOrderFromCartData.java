package io.hhplus.ECommerce.ECommerce_project.order.application.dto;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ValidatedOrderFromCartData(
        List<Cart> cartList,
        List<Map.Entry<Long, Integer>> sortedEntries,
        Map<Long, Product> productMap,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount
) {}
