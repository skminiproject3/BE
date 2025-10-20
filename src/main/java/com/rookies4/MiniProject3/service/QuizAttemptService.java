package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;

    // 새로운 퀴즈 제출 기록 저장
    public QuizAttempt saveAttempt(User user, Quiz quiz, String selectedAnswer, Boolean isCorrect) {
        QuizAttempt attempt = QuizAttempt.builder()
                .user(user)
                .quiz(quiz)
                .selectedAnswer(selectedAnswer)
                .isCorrect(isCorrect)
                .build();
        return quizAttemptRepository.save(attempt);
    }

    // 특정 사용자가 특정 업로드 문서 퀴즈 기록 조회
    public List<QuizAttempt> getUserAttemptsByUpload(User user, Long uploadId) {
        return quizAttemptRepository.findByUserAndQuiz_Upload_Id(user, uploadId);
    }

    // 특정 사용자가 특정 퀴즈 문제를 푼 기록 조회
    public List<QuizAttempt> getUserAttemptsByQuiz(User user, Quiz quiz) {
        return quizAttemptRepository.findByUserAndQuiz(user, quiz);
    }
}