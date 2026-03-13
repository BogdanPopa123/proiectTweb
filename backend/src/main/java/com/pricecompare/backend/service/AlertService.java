package com.pricecompare.backend.service;

import com.pricecompare.backend.dto.request.AlertRequest;
import com.pricecompare.backend.dto.response.AlertResponse;
import com.pricecompare.backend.entity.Alert;
import com.pricecompare.backend.entity.Product;
import com.pricecompare.backend.entity.User;
import com.pricecompare.backend.exception.BadRequestException;
import com.pricecompare.backend.exception.ResourceNotFoundException;
import com.pricecompare.backend.repository.AlertRepository;
import com.pricecompare.backend.repository.ProductRepository;
import com.pricecompare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Page<AlertResponse> getUserAlerts(Long userId, Pageable pageable) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(AlertResponse::from);
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long id, Long userId) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        if (!alert.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Alert", id);
        }
        return AlertResponse.from(alert);
    }

    @Transactional
    public AlertResponse createAlert(Long userId, AlertRequest request) {
        if (alertRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BadRequestException("Alert already exists for this product");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        Alert alert = Alert.builder()
                .user(user)
                .product(product)
                .targetPrice(request.getTargetPrice())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        return AlertResponse.from(alertRepository.save(alert));
    }

    @Transactional
    public AlertResponse updateAlert(Long id, Long userId, AlertRequest request) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        if (!alert.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Alert", id);
        }

        if (request.getTargetPrice() != null) alert.setTargetPrice(request.getTargetPrice());
        if (request.getEnabled() != null) alert.setEnabled(request.getEnabled());

        return AlertResponse.from(alertRepository.save(alert));
    }

    @Transactional
    public void deleteAlert(Long id, Long userId) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        if (!alert.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Alert", id);
        }
        alertRepository.delete(alert);
    }

    @Transactional
    public void checkAndTriggerAlerts(Long productId, BigDecimal newPrice, BigDecimal oldPrice) {
        if (oldPrice == null || newPrice.compareTo(oldPrice) >= 0) {
            return; // Price didn't drop
        }

        List<Alert> triggeredAlerts = alertRepository.findTriggeredAlerts(productId, newPrice);

        for (Alert alert : triggeredAlerts) {
            try {
                alert.setTriggeredAt(LocalDateTime.now());
                alertRepository.save(alert);

                // Send email notification
                emailService.sendPriceDropAlert(
                        alert.getUser().getEmail(),
                        alert.getUser().getUsername(),
                        alert.getProduct().getName(),
                        alert.getProduct().getProductUrl(),
                        oldPrice,
                        newPrice,
                        alert.getProduct().getStore() != null
                                ? alert.getProduct().getStore().getName() : "Unknown"
                );
            } catch (Exception e) {
                log.error("Failed to process alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }
}
