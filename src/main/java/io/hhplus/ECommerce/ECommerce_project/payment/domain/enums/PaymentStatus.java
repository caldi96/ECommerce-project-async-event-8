package io.hhplus.ECommerce.ECommerce_project.payment.domain.enums;

public enum PaymentStatus {

    PENDING("결제대기", "결제 대기 중입니다."),
    COMPLETED("결제완료", "결제가 완료되었습니다."),
    FAILED("결제실패", "결제 처리 중 오류가 발생했습니다. 보상 처리가 진행 중입니다."),
    REFUNDED("환불완료", "환불이 완료되었습니다.");

    private final String description;
    private final String message;

    PaymentStatus(String description, String message) {
        this.description = description;
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 사용자에게 보여줄 응답 메시지를 반환
     */
    public String getResponseMessage() {
        return message;
    }
}
