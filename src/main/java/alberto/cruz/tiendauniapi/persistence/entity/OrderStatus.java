package alberto.cruz.tiendauniapi.persistence.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PENDING_PROOF_VERIFICATION,
    PAID_PENDING_DELIVERY,
    COMPLETED,
    CANCELLED
}
