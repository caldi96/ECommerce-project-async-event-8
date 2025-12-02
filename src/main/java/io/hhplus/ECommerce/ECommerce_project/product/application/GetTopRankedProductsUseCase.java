package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTopRankedProductsUseCase {

    private final ProductFinderService productFinderService;

    public List<Product> execute() {
        Pageable pageable = PageRequest.of(0, 20); // 1페이지 상위 20개만 조회
        return productFinderService.getTop20Products(pageable);
    }
}
