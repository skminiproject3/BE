package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.QuestionDto;
import com.rookies4.MiniProject3.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents/{contentId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionDto.Response> askQuestion(
            @PathVariable Long contentId,
            @RequestBody QuestionDto.Request request
    ) {
        QuestionDto.Response response = questionService.askQuestion(contentId, request);
        return ResponseEntity.ok(response);
    }
}
