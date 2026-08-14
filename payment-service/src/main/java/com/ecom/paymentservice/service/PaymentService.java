package com.ecom.paymentservice.service;

import com.ecom.paymentservice.dto.PaymentRequest;
import com.ecom.paymentservice.dto.PaymentResponse;
import com.ecom.paymentservice.entity.Payment;
import com.ecom.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // Simple mock rule: any amount above 0 succeeds, exact amount 999 simulates a failure (for demo/testing)
    public PaymentResponse process(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        boolean isSimulatedFailure = request.getAmount().compareTo(new BigDecimal("999")) == 0;
        payment.setStatus(isSimulatedFailure ? Payment.PaymentStatus.FAILED : Payment.PaymentStatus.SUCCESS);
        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No payment found for order: " + orderId));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus().name(), p.getCreatedAt());
    }
}
