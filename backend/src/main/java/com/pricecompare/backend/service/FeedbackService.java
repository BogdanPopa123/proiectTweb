package com.pricecompare.backend.service;

import com.pricecompare.backend.dto.request.FeedbackRequest;
import com.pricecompare.backend.dto.response.FeedbackResponse;
import com.pricecompare.backend.entity.Feedback;
import com.pricecompare.backend.entity.User;
import com.pricecompare.backend.exception.ResourceNotFoundException;
import com.pricecompare.backend.repository.FeedbackRepository;
import com.pricecompare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedback(Pageable pageable) {
        return feedbackRepository.findAll(pageable).map(FeedbackResponse::from);
    }

    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Long id) {
        return feedbackRepository.findById(id)
                .map(FeedbackResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", id));
    }

    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request, Long userId) {
        User user = userId != null
                ? userRepository.findById(userId).orElse(null)
                : null;

        Feedback feedback = Feedback.builder()
                .user(user)
                .category(request.getCategory())
                .rating(request.getRating())
                .subscribeNewsletter(request.getSubscribeNewsletter() != null
                        ? request.getSubscribeNewsletter() : false)
                .message(request.getMessage())
                .build();

        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Transactional
    public void deleteFeedback(Long id) {
        if (!feedbackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Feedback", id);
        }
        feedbackRepository.deleteById(id);
    }
}
