package com.rookies4.MiniProject3.dto;

import lombok.*;
import java.util.List;

public class QuizAttemptDto {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private List<Answer> answers;

        @Getter @Setter
        @NoArgsConstructor @AllArgsConstructor
        public static class Answer {
            private Long quizId;
            private String submittedAnswer;
        }
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long attemptId;
        private int totalQuestions;
        private int correctAnswers;
        private double score;
        private List<Result> results;

        @Getter @Setter
        @NoArgsConstructor @AllArgsConstructor @Builder
        public static class Result {
            private Long quizId;
            private boolean isCorrect;
            private String correctAnswer;
            private String explanation;
            private String difficulty;
        }
    }
}
