package com.rookies4.MiniProject3.dto.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiQuestionResponse {
    private String source;
    private Long content_id;
    private String question;
    private String answer;
    private String message;
}