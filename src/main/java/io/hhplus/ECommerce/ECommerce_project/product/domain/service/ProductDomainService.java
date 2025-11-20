package io.hhplus.ECommerce.ECommerce_project.product.domain.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import org.springframework.stereotype.Component;

@Component
public class ProductDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_ID_INVALID);
        }
    }
}
