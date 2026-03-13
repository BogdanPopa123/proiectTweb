package com.pricecompare.backend.repository;

import com.pricecompare.backend.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdOrderByRecordedAtDesc(Long productId);

    Page<PriceHistory> findByProductIdOrderByRecordedAtDesc(Long productId, Pageable pageable);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.product.id = :productId " +
           "ORDER BY ph.recordedAt DESC LIMIT 1")
    Optional<PriceHistory> findLatestByProductId(@Param("productId") Long productId);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.product.id = :productId " +
           "AND ph.recordedAt >= :since ORDER BY ph.recordedAt ASC")
    List<PriceHistory> findByProductIdSince(@Param("productId") Long productId,
                                             @Param("since") LocalDateTime since);

    @Query("SELECT MIN(ph.price) FROM PriceHistory ph WHERE ph.product.id = :productId")
    Optional<java.math.BigDecimal> findMinPriceByProductId(@Param("productId") Long productId);

    @Query("SELECT MAX(ph.price) FROM PriceHistory ph WHERE ph.product.id = :productId")
    Optional<java.math.BigDecimal> findMaxPriceByProductId(@Param("productId") Long productId);
}
