package com.rookies4.MiniProject3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class QuizDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private int count;
        private String difficulty;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {
        private String message;
        private int quizCount;
    }
}
