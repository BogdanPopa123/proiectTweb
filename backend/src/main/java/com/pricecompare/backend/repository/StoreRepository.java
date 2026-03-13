package com.pricecompare.backend.repository;

import com.pricecompare.backend.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByName(String name);

    List<Store> findByActiveTrue();

    @Query("SELECT s FROM Store s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.baseUrl) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Store> searchStores(@Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM Store s ORDER BY s.name ASC")
    Page<Store> findAllOrderByName(Pageable pageable);
}
