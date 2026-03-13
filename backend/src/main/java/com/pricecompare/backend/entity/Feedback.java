package com.pricecompare.backend.entity;

import com.pricecompare.backend.entity.enums.FeedbackCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One: Feedback -> User (nullable — anonymous feedback allowed)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // SELECT field
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeedbackCategory category;

    // RADIO field (1-5 stars)
    @Column(nullable = false)
    private Integer rating;

    // CHECKBOX field
    @Column(name = "subscribe_newsletter", nullable = false)
    @Builder.Default
    private Boolean subscribeNewsletter = false;

    // TEXTAREA field
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
