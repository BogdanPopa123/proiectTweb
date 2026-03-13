package com.pricecompare.backend.repository;

import com.pricecompare.backend.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByUserId(Long userId, Pageable pageable);

    List<Alert> findByUserIdAndEnabledTrue(Long userId);

    Optional<Alert> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT a FROM Alert a WHERE a.enabled = true AND " +
           "a.product.id = :productId AND " +
           "(a.targetPrice IS NULL OR a.targetPrice >= :currentPrice)")
    List<Alert> findTriggeredAlerts(@Param("productId") Long productId,
                                    @Param("currentPrice") BigDecimal currentPrice);

    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<Alert> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
