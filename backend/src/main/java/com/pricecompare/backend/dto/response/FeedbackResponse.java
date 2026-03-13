package com.pricecompare.backend.dto.response;

import com.pricecompare.backend.entity.Feedback;
import com.pricecompare.backend.entity.enums.FeedbackCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String username;
    private FeedbackCategory category;
    private Integer rating;
    private Boolean subscribeNewsletter;
    private String message;
    private LocalDateTime createdAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .userId(feedback.getUser() != null ? feedback.getUser().getId() : null)
                .username(feedback.getUser() != null ? feedback.getUser().getUsername() : "Anonymous")
                .category(feedback.getCategory())
                .rating(feedback.getRating())
                .subscribeNewsletter(feedback.getSubscribeNewsletter())
                .message(feedback.getMessage())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
