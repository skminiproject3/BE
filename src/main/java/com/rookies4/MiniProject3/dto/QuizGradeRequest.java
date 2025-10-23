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
        private String question;      // ✅ FastAPI가 기대하는 이름으로 변경
        private String user_answer;   // ✅ userAnswer → user_answer
    }
}
