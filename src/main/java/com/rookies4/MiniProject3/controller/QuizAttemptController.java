package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.QuizAttemptDto;
import com.rookies4.MiniProject3.service.QuizAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents/{contentId}/quizzes/attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping
    public ResponseEntity<QuizAttemptDto.Response> submitQuizAttempt(
            @PathVariable Long contentId,
            @RequestBody QuizAttemptDto.Request request
    ) {
        QuizAttemptDto.Response response = quizAttemptService.evaluateQuiz(contentId, request);
        return ResponseEntity.ok(response);
    }
}
