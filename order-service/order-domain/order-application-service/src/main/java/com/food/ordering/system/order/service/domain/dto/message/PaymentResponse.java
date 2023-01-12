package com.food.ordering.system.order.service.domain.dto.message;

import com.food.ordering.system.domain.valueobject.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private String id;

    private String sagaId;

    private String orderId;

    private String paymentId;

    private String customerId;

    private BigDecimal price;

    private Instant createdAt;

    private PaymentStatus paymentStatus;

    private List<String> failureMessages;

}
