package com.rookies4.MiniProject3.dto;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// GradeResponse.java
@Getter
@Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class QuizGradeResponseDto {
    private String status;
    private String message;
    private Long attempt_id;
    private Long content_id;
    private Integer batch;
    private Integer total_questions;
    private Integer correct_count;
    private Integer final_total_score;
    private List<ResultRow> results;

    @Getter @Setter @Builder @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultRow {
        private Long quiz_id;
        private String question;
        private List<String> options;
        private String correct_answer;
        private String user_answer;
        private Boolean is_correct;
    }

    // 컨트롤러에서 바로 반환 가능한 Map 형태로도 제공
    public Map<String, Object> toApi() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("message", message);
        m.put("attempt_id", attempt_id);
        m.put("content_id", content_id);
        m.put("batch", batch);
        m.put("total_questions", total_questions);
        m.put("correct_count", correct_count);
        m.put("final_total_score", final_total_score);
        m.put("results", results);
        return m;
    }
}
