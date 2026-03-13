package com.pricecompare.backend.service;

import com.pricecompare.backend.dto.response.PriceHistoryResponse;
import com.pricecompare.backend.entity.PriceHistory;
import com.pricecompare.backend.entity.Product;
import com.pricecompare.backend.exception.ResourceNotFoundException;
import com.pricecompare.backend.repository.PriceHistoryRepository;
import com.pricecompare.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getHistoryForProduct(Long productId) {
        return priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(productId)
                .stream().map(PriceHistoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<PriceHistoryResponse> getHistoryForProductPaged(Long productId, Pageable pageable) {
        return priceHistoryRepository
                .findByProductIdOrderByRecordedAtDesc(productId, pageable)
                .map(PriceHistoryResponse::from);
    }

    @Transactional
    public PriceHistory recordPrice(Long productId, BigDecimal price, String currency) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        PriceHistory priceHistory = PriceHistory.builder()
                .product(product)
                .price(price)
                .currency(currency != null ? currency : "RON")
                .build();

        PriceHistory saved = priceHistoryRepository.save(priceHistory);

        // Update current price on product
        product.setCurrentPrice(price);
        productRepository.save(product);

        return saved;
    }

    @Transactional
    public void deletePriceHistory(Long id) {
        if (!priceHistoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("PriceHistory", id);
        }
        priceHistoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getMinPrice(Long productId) {
        return priceHistoryRepository.findMinPriceByProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getHistorySince(Long productId, LocalDateTime since) {
        return priceHistoryRepository.findByProductIdSince(productId, since)
                .stream().map(PriceHistoryResponse::from).toList();
    }
}
