package com.rookies4.MiniProject3.dto;

import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class QuizAttemptDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long attemptId;
        private Float score;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private LocalDateTime createdAt;

        public static Response fromEntity(QuizAttempt attempt) {
            return Response.builder()
                    .attemptId(attempt.getId())
                    .score(attempt.getScore())
                    .totalQuestions(attempt.getTotalQuestions())
                    .correctAnswers(attempt.getCorrectAnswers())
                    .createdAt(attempt.getCreatedAt())
                    .build();
        }
    }
}
