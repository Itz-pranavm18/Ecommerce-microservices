package com.ecom.orderservice.service;

import com.ecom.orderservice.client.PaymentClient;
import com.ecom.orderservice.client.ProductClient;
import com.ecom.orderservice.dto.*;
import com.ecom.orderservice.entity.Order;
import com.ecom.orderservice.entity.OrderItem;
import com.ecom.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    /*
     * Simple happy-path saga (no distributed rollback/compensation logic — kept
     * intentionally simple for a learning project):
     *   1. Fetch each product via Feign -> validate stock
     *   2. Reduce stock via Feign for each item
     *   3. Call Payment Service via Feign
     *   4. Persist the order with the resulting status
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(Order.OrderStatus.PLACED);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductDto product = productClient.getProduct(itemRequest.getProductId());

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName());
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            order.getItems().add(orderItem);

            // Reduce stock on the Product Service via Feign (load-balanced by Eureka)
            productClient.reduceStock(product.getId(), itemRequest.getQuantity());
        }

        order.setTotalAmount(total);

        // Save first so we have an order ID to send to the Payment Service
        Order saved = orderRepository.save(order);

        // Call Payment Service via Feign
        PaymentResponseDto paymentResponse = paymentClient.processPayment(
                new PaymentRequestDto(saved.getId(), total));

        if (!"SUCCESS".equals(paymentResponse.getStatus())) {
            saved.setStatus(Order.OrderStatus.PAYMENT_FAILED);
            saved = orderRepository.save(saved);
        }

        return toResponse(saved);
    }

    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return toResponse(order);
    }

    public List<OrderResponse> getByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProductId(), i.getProductName(), i.getQuantity(), i.getPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getTotalAmount(),
                order.getStatus().name(), order.getCreatedAt(), items);
    }
}
