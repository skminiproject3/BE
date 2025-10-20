package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.enums.Difficulty;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/create")
    public ResponseEntity<Quiz> createQuiz(@RequestParam Upload upload,
                                           @RequestParam String question,
                                           @RequestParam String answer,
                                           @RequestParam(required = false) String optionsJson,
                                           @RequestParam(defaultValue = "EASY") Difficulty difficulty) {
        Quiz quiz = quizService.saveQuiz(upload, question, answer, optionsJson, difficulty);
        return ResponseEntity.ok(quiz);
    }

    @GetMapping("/upload/{uploadId}")
    public ResponseEntity<List<Quiz>> getQuizzes(@PathVariable Upload upload) {
        List<Quiz> quizzes = quizService.getQuizzesByUpload(upload);
        return ResponseEntity.ok(quizzes);
    }
}