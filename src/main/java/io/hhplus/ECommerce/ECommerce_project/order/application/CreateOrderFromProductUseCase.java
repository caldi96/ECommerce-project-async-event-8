package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.event.OrderValidationRequestedEvent;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 단일 상품 주문 생성 UseCase
 * - Redis 재고 차감 후 즉시 응답 반환
 * - 실제 주문 처리는 비동기 이벤트로 진행
 */
@Service
@RequiredArgsConstructor
public class CreateOrderFromProductUseCase {

    private final RedisStockService redisStockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CreateOrderResponse execute(CreateOrderFromProductCommand command) {

        try {
            // 1. Redis 재고 차감 (동기)
            redisStockService.decreaseStock(command.productId(), command.quantity());

            // 2. 검증 및 계산 이벤트 발행 (비동기)
            applicationEventPublisher.publishEvent(
                    OrderValidationRequestedEvent.of(command)
            );

            // 3. 주문 접수 완료 응답 즉시 반환
            return CreateOrderResponse.accepted(
                    command.userId(),
                    command.productId(),
                    command.quantity()
            );

        } catch (Exception e) {
            // Redis 재고 차감 실패 시 예외 발생
            throw e;
        }
    }
}