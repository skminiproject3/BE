package com.rookies4.MiniProject3.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionRequest {
    private String question;
    private boolean forceWeb; // 기본 false
}