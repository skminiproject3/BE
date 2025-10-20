package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;

    // 새로운 퀴즈 제출 기록 저장
    public QuizAttempt submitQuiz(User user, Quiz quiz, String selectedAnswer) {
        boolean isCorrect = quiz.getAnswer().equals(selectedAnswer);
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

    // 점수 계산
    public double calculateScore(User user, Long uploadId) {
        List<QuizAttempt> attempts = getUserAttemptsByUpload(user, uploadId);
        if (attempts.isEmpty()) return 0.0;
        long correctCount = attempts.stream().filter(QuizAttempt::getIsCorrect).count();
        return (double) correctCount / attempts.size() * 100;
    }

    // 오답 확인
    public List<QuizAttempt> getWrongAnswers(User user, Long uploadId) {
        return getUserAttemptsByUpload(user, uploadId)
                .stream()
                .filter(a -> !a.getIsCorrect())
                .collect(Collectors.toList());
    }
}