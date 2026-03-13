package com.pricecompare.backend.service;

import com.pricecompare.backend.dto.request.StoreRequest;
import com.pricecompare.backend.dto.response.StoreResponse;
import com.pricecompare.backend.entity.Store;
import com.pricecompare.backend.exception.ResourceNotFoundException;
import com.pricecompare.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional(readOnly = true)
    public Page<StoreResponse> getAllStores(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return storeRepository.searchStores(search, pageable).map(StoreResponse::from);
        }
        return storeRepository.findAll(pageable).map(StoreResponse::from);
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        return storeRepository.findById(id)
                .map(StoreResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Store", id));
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getActiveStores() {
        return storeRepository.findByActiveTrue().stream()
                .map(StoreResponse::from)
                .toList();
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        Store store = Store.builder()
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .logoUrl(request.getLogoUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public StoreResponse updateStore(Long id, StoreRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store", id));

        if (StringUtils.hasText(request.getName())) store.setName(request.getName());
        if (StringUtils.hasText(request.getBaseUrl())) store.setBaseUrl(request.getBaseUrl());
        if (request.getLogoUrl() != null) store.setLogoUrl(request.getLogoUrl());
        if (request.getActive() != null) store.setActive(request.getActive());

        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public void deleteStore(Long id) {
        if (!storeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Store", id);
        }
        storeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Store getStoreEntityById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store", id));
    }
}
