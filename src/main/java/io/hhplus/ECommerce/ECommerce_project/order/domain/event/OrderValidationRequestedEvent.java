package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;

public record OrderValidationRequestedEvent(
        CreateOrderFromProductCommand command
) {
    public static OrderValidationRequestedEvent of(CreateOrderFromProductCommand command) {
        return new OrderValidationRequestedEvent(command);
    }
}
