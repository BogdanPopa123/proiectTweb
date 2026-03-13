package com.pricecompare.backend.service;

import com.pricecompare.backend.dto.request.ProductRequest;
import com.pricecompare.backend.dto.response.ProductResponse;
import com.pricecompare.backend.entity.Product;
import com.pricecompare.backend.entity.Store;
import com.pricecompare.backend.exception.ResourceNotFoundException;
import com.pricecompare.backend.repository.ProductRepository;
import com.pricecompare.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return productRepository.searchProducts(search, pageable).map(ProductResponse::from);
        }
        return productRepository.findByActiveTrue(pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByStore(Long storeId, Pageable pageable) {
        return productRepository.findByStoreId(storeId, pageable).map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .productUrl(request.getProductUrl())
                .category(request.getCategory())
                .store(store)
                .currentPrice(request.getCurrentPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "RON")
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (StringUtils.hasText(request.getName())) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (StringUtils.hasText(request.getProductUrl())) product.setProductUrl(request.getProductUrl());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getCurrentPrice() != null) product.setCurrentPrice(request.getCurrentPrice());
        if (request.getCurrency() != null) product.setCurrency(request.getCurrency());
        if (request.getActive() != null) product.setActive(request.getActive());

        if (request.getStoreId() != null) {
            Store store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));
            product.setStore(store);
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
