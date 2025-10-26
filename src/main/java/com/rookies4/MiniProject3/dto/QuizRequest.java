package com.rookies4.MiniProject3.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizRequest {
    private int numQuestions = 5;
    private String difficulty = "MEDIUM";
}