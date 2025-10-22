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

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final ContentRepository contentRepository;
    private final QuizRepository quizRepository;

    public QuizAttemptDto.Response evaluateQuiz(Long contentId, QuizAttemptDto.Request request) {

        // ✅ 1. 콘텐츠 유효성 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        // ✅ 2. 해당 콘텐츠의 퀴즈 전체 조회
        List<Quiz> quizzes = quizRepository.findByContent(content);
        if (quizzes.isEmpty()) {
            throw new CustomException(ErrorCode.QUIZ_NOT_FOUND);
        }

        int total = request.getAnswers().size();
        int correctCount = 0;
        List<QuizAttemptDto.Response.Result> results = new ArrayList<>();

        // ✅ 3. 사용자 답안 비교
        for (QuizAttemptDto.Request.Answer answer : request.getAnswers()) {
            Quiz quiz = quizzes.stream()
                    .filter(q -> q.getId().equals(answer.getQuizId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

            String correctAnswer = quiz.getCorrectAnswer() != null ? quiz.getCorrectAnswer().trim() : "";
            String userAnswer = answer.getSubmittedAnswer() != null ? answer.getSubmittedAnswer().trim() : "";

            boolean isCorrect = false;

            // 숫자/기호 차이 제거 (예: "1" vs "1.", "①" vs "1")
            if (!correctAnswer.isBlank()) {
                String normalizedCorrect = correctAnswer.replaceAll("[^0-9a-zA-Z]", "");
                String normalizedUser = userAnswer.replaceAll("[^0-9a-zA-Z]", "");
                isCorrect = normalizedCorrect.equalsIgnoreCase(normalizedUser);
            }

            if (isCorrect) correctCount++;

            // ✅ 맞춘 경우: explanation 필드 제외, 틀린 경우: 정답 안내 포함
            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("quizId", quiz.getId());
            resultData.put("correctAnswer", correctAnswer);
            resultData.put("difficulty", quiz.getDifficulty() != null ? quiz.getDifficulty().name() : "UNKNOWN");
            resultData.put("correct", isCorrect);

            if (!isCorrect) {
                resultData.put("explanation", "정답은 " + correctAnswer + "번입니다.");
            }

            // ✅ QuizAttemptDto.Response.Result로 변환
            QuizAttemptDto.Response.Result result = QuizAttemptDto.Response.Result.builder()
                    .quizId((Long) resultData.get("quizId"))
                    .isCorrect((Boolean) resultData.get("correct"))
                    .correctAnswer((String) resultData.get("correctAnswer"))
                    .explanation((String) resultData.getOrDefault("explanation", null))
                    .difficulty((String) resultData.get("difficulty"))
                    .build();

            results.add(result);
        }

        // ✅ 4. 점수 계산
        double score = total > 0 ? ((double) correctCount / total) * 100.0 : 0.0;

        // ✅ 5. 결과 반환
        return QuizAttemptDto.Response.builder()
                .attemptId(System.currentTimeMillis())
                .totalQuestions(total)
                .correctAnswers(correctCount)
                .score(score)
                .results(results)
                .build();
    }
}
