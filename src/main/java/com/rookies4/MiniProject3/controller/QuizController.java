package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.QuizDto;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contents/{contentId}/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> generateQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizDto.Request request
    ) {
        List<Map<String, Object>> quizzes = quizService.generateQuiz(contentId, request);
        return ResponseEntity.ok(quizzes);
    }
}
