package com.ecom.orderservice.client;

import com.ecom.orderservice.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// "PRODUCT-SERVICE" resolves via Eureka; Spring Cloud LoadBalancer picks an instance
// automatically, so running multiple product-service instances load-balances for free.
@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    @PatchMapping("/api/products/{id}/reduce-stock")
    ProductDto reduceStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}
