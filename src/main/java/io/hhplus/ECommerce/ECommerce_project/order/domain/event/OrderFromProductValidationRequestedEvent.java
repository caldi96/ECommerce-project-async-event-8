package io.hhplus.ECommerce.ECommerce_project.order.domain.event;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;

public record OrderFromProductValidationRequestedEvent(
        CreateOrderFromProductCommand command
) {
    public static OrderFromProductValidationRequestedEvent of(CreateOrderFromProductCommand command) {
        return new OrderFromProductValidationRequestedEvent(command);
    }
}
