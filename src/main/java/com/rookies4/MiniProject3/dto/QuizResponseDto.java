package com.rookies4.MiniProject3.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponseDto {

    private String question;
    private List<String> options;

    @JsonProperty("correct_answer")
    private String correctAnswer;

    private String explanation;
}