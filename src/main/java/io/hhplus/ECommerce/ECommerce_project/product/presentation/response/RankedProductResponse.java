package io.hhplus.ECommerce.ECommerce_project.product.presentation.response;

public record RankedProductResponse(
        Integer rank,
        ProductResponse product
) {
}
