package io.hhplus.ECommerce.ECommerce_project.order.application.dto;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 장바구니 주문 검증 완료 데이터
 */
public record ValidatedOrderFromCartData(
        User user,
        List<Cart> cartList,
        List<Map.Entry<Long, Integer>> sortedEntries,
        Map<Long, Product> productMap,
        Coupon coupon,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal pointAmount,
        BigDecimal finalAmount
) {}
