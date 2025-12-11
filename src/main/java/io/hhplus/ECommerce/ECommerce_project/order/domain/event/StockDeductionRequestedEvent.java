package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromProductData;

public record StockDeductionRequestedEvent(
        CreateOrderFromProductCommand command,
        ValidatedOrderFromProductData validatedOrderFromProductData
) {
    public static StockDeductionRequestedEvent of(
            CreateOrderFromProductCommand command,
            ValidatedOrderFromProductData validatedOrderFromProductData
    ) {
        return new StockDeductionRequestedEvent(command, validatedOrderFromProductData);
    }
}
