package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.service.QuizAttemptService;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    // 특정 업로드 문서 퀴즈 조회
    @GetMapping("/upload/{uploadId}")
    public List<Quiz> getQuizzesByUpload(@PathVariable Long uploadId) {
        Content content = content.builder().id(uploadId).build(); // 예시, 실제로는 DB 조회
        return quizService.getQuizzesByUpload(content);
    }

    // 퀴즈 제출
    @PostMapping("/submit/{quizId}")
    public QuizAttempt submitQuiz(@PathVariable Long quizId,
                                  @RequestParam String selectedAnswer,
                                  @RequestParam Long userId) {
        User user = User.builder().id(userId).build(); // 예시, 실제로는 DB 조회
        Quiz quiz = Quiz.builder().id(quizId).build(); // 예시, 실제로는 DB 조회
        return quizAttemptService.submitQuiz(user, quiz, selectedAnswer);
    }

    // 점수 확인
    @GetMapping("/score/{uploadId}/{userId}")
    public double getScore(@PathVariable Long uploadId, @PathVariable Long userId) {
        User user = User.builder().id(userId).build();
        return quizAttemptService.calculateScore(user, uploadId);
    }

    // 오답 확인
    @GetMapping("/wrong/{uploadId}/{userId}")
    public List<QuizAttempt> getWrongAnswers(@PathVariable Long uploadId, @PathVariable Long userId) {
        User user = User.builder().id(userId).build();
        return quizAttemptService.getWrongAnswers(user, uploadId);
    }
}