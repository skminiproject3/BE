package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.dto.QuizAttemptDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final ContentRepository contentRepository;
    private final QuizRepository quizRepository;

    public QuizAttemptDto.Response evaluateQuiz(Long contentId, QuizAttemptDto.Request request) {

        // ✅ 1. Content 존재 여부 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        // ✅ 2. 해당 콘텐츠의 퀴즈 전체 조회
        List<Quiz> quizzes = quizRepository.findByContent(content);
        if (quizzes.isEmpty()) {
            throw new CustomException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // ✅ 3. 요청된 답안 처리
        int total = request.getAnswers().size();
        int correct = 0;
        List<QuizAttemptDto.Response.Result> results = new ArrayList<>();

        for (QuizAttemptDto.Request.Answer answer : request.getAnswers()) {
            Quiz quiz = quizzes.stream()
                    .filter(q -> q.getId().equals(answer.getQuizId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

            boolean isCorrect = quiz.getCorrectAnswer().trim()
                    .equalsIgnoreCase(answer.getSubmittedAnswer().trim());
            if (isCorrect) correct++;

            results.add(
                    QuizAttemptDto.Response.Result.builder()
                            .quizId(quiz.getId())
                            .isCorrect(isCorrect)
                            .correctAnswer(quiz.getCorrectAnswer())
                            .explanation(quiz.getExplanation() != null ? quiz.getExplanation() : "")
                            .difficulty(quiz.getDifficulty() != null ? quiz.getDifficulty().name() : "UNKNOWN")
                            .build()
            );
        }

        double score = total > 0 ? ((double) correct / total) * 100.0 : 0.0;

        // ✅ 4. 결과 응답 반환
        return QuizAttemptDto.Response.builder()
                .attemptId(System.currentTimeMillis()) // 실제 DB 저장 시 대체 가능
                .totalQuestions(total)
                .correctAnswers(correct)
                .score(score)
                .results(results)
                .build();
    }
}
