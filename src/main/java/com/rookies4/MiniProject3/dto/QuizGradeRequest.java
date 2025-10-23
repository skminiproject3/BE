package com.rookies4.MiniProject3.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class QuizGradeRequest {
    private List<Answer> answers;

    @Getter
    @Setter
    public static class Answer {
        private String questionText;
        private String userAnswer;
    }
}