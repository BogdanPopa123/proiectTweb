package com.pricecompare.backend.repository;

import com.pricecompare.backend.entity.Feedback;
import com.pricecompare.backend.entity.enums.FeedbackCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByCategory(FeedbackCategory category, Pageable pageable);

    Page<Feedback> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM Feedback f")
    Double findAverageRating();

    @Query("SELECT f FROM Feedback f WHERE f.subscribeNewsletter = true")
    Page<Feedback> findSubscribers(Pageable pageable);
}
