package com.pricecompare.backend.repository;

import com.pricecompare.backend.entity.ProductGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductGroupRepository extends JpaRepository<ProductGroup, Long> {

    @Query("SELECT pg FROM ProductGroup pg WHERE " +
           "LOWER(pg.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ProductGroup> searchGroups(@Param("search") String search, Pageable pageable);

    @Query("SELECT pg FROM ProductGroup pg JOIN pg.products p WHERE p.id = :productId")
    Optional<ProductGroup> findByProductId(@Param("productId") Long productId);

    @Query("SELECT pg FROM ProductGroup pg JOIN FETCH pg.products WHERE pg.id = :id")
    Optional<ProductGroup> findByIdWithProducts(@Param("id") Long id);
}
