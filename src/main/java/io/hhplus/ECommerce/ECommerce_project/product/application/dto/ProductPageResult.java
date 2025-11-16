package io.hhplus.ECommerce.ECommerce_project.product.application.dto;

import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductPageResult {
    private final List<Product> products;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;

    public ProductPageResult(List<Product> products, int page, int size, long totalElements, int totalPages, boolean isFirst, boolean isLast) {
        this.products = products;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }
}