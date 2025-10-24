package com.rookies4.MiniProject3.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuizGradeRequest {

    private List<Answer> answers;

    @Getter
    @Setter
    public static class Answer {
        private Long quiz_id;        // ✅ 새로 추가됨 (quiz_id 기반 채점용)
        private String question;     // 기존 question 필드 (호환용)
        private String user_answer;  // 사용자가 선택한 답변
    }
}

