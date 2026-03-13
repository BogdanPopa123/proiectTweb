package com.pricecompare.backend.repository;

import com.pricecompare.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductUrl(String productUrl);

    boolean existsByProductUrl(String productUrl);

    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.store.id = :storeId AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> searchProductsByStore(@Param("search") String search,
                                        @Param("storeId") Long storeId,
                                        Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.phash IS NOT NULL AND p.active = true")
    List<Product> findAllWithPhash();

    @Query("SELECT p FROM Product p JOIN p.watchers u WHERE u.id = :userId")
    Page<Product> findWatchlistByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.active = true")
    Page<Product> findByCategory(@Param("category") String category, Pageable pageable);
}
