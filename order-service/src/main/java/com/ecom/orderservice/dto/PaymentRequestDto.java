package com.ecom.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentRequestDto {
    private Long orderId;
    private BigDecimal amount;
}
