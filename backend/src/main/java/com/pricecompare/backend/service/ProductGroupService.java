package com.pricecompare.backend.service;

import com.pricecompare.backend.dto.request.ProductGroupRequest;
import com.pricecompare.backend.dto.response.ProductGroupResponse;
import com.pricecompare.backend.entity.Product;
import com.pricecompare.backend.entity.ProductGroup;
import com.pricecompare.backend.exception.ResourceNotFoundException;
import com.pricecompare.backend.repository.ProductGroupRepository;
import com.pricecompare.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductGroupService {

    private final ProductGroupRepository productGroupRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductGroupResponse> getAllGroups(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return productGroupRepository.searchGroups(search, pageable)
                    .map(ProductGroupResponse::from);
        }
        return productGroupRepository.findAll(pageable).map(ProductGroupResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductGroupResponse getGroupById(Long id) {
        return productGroupRepository.findByIdWithProducts(id)
                .map(ProductGroupResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("ProductGroup", id));
    }

    @Transactional
    public ProductGroupResponse createGroup(ProductGroupRequest request) {
        Set<Product> products = new HashSet<>();
        if (request.getProductIds() != null) {
            for (Long pid : request.getProductIds()) {
                productRepository.findById(pid).ifPresent(products::add);
            }
        }

        ProductGroup group = ProductGroup.builder()
                .name(request.getName())
                .products(products)
                .build();

        return ProductGroupResponse.from(productGroupRepository.save(group));
    }

    @Transactional
    public ProductGroupResponse updateGroup(Long id, ProductGroupRequest request) {
        ProductGroup group = productGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductGroup", id));

        if (StringUtils.hasText(request.getName())) {
            group.setName(request.getName());
        }

        if (request.getProductIds() != null) {
            Set<Product> products = new HashSet<>();
            for (Long pid : request.getProductIds()) {
                productRepository.findById(pid).ifPresent(products::add);
            }
            group.setProducts(products);
        }

        return ProductGroupResponse.from(productGroupRepository.save(group));
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!productGroupRepository.existsById(id)) {
            throw new ResourceNotFoundException("ProductGroup", id);
        }
        productGroupRepository.deleteById(id);
    }
}
