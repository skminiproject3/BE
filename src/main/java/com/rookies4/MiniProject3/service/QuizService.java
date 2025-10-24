package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.dto.QuizGradeRequest;
import com.rookies4.MiniProject3.dto.QuizResponseDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.QuizAttemptRepository;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================================
    // ✅ 1. 퀴즈 저장 (quiz_id 자동 생성)
    // ==========================================================
    public Quiz saveQuiz(Content content, String question, String correctAnswer, String optionsJson, String explanation) {
        try {
            if (content == null) {
                throw new CustomException(ErrorCode.CONTENT_NOT_FOUND);
            }

            int nextQuizId = quizRepository.findTopByContentOrderByQuizIdDesc(content)
                    .map(q -> q.getQuizId() + 1)
                    .orElse(1);

            Quiz quiz = Quiz.builder()
                    .content(content)
                    .quizId(nextQuizId)
                    .question(question)
                    .options(optionsJson)
                    .correctAnswer(correctAnswer)
                    .explanation(explanation)
                    .build();

            return quizRepository.save(quiz);

        } catch (DataAccessException e) {
            log.error("❌ DB 오류: 퀴즈 저장 실패 - {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        } catch (Exception e) {
            log.error("❌ 예기치 못한 오류 (saveQuiz): {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // ✅ 2. FastAPI에서 생성된 퀴즈 리스트 저장
    // ==========================================================
    public int saveGeneratedQuizzes(Content content, List<QuizResponseDto> quizzes) {
        if (quizzes == null || quizzes.isEmpty()) return 0;

        int count = 0;
        for (QuizResponseDto dto : quizzes) {
            try {
                String optionsJson = objectMapper.writeValueAsString(dto.getOptions());
                saveQuiz(content, dto.getQuestion(), dto.getCorrectAnswer(), optionsJson, dto.getExplanation());
                count++;
            } catch (JsonProcessingException e) {
                log.warn("⚠️ JSON 직렬화 실패 (question='{}')", dto.getQuestion());
            } catch (CustomException e) {
                log.error("⚠️ 퀴즈 저장 실패 (CustomException): {}", e.getErrorCode());
            } catch (Exception e) {
                log.error("⚠️ 퀴즈 저장 중 알 수 없는 오류: {}", e.getMessage(), e);
            }
        }
        return count;
    }

    // ==========================================================
    // ✅ 3. 콘텐츠별 퀴즈 조회
    // ==========================================================
    public List<Quiz> getQuizzesByContent(Content content) {
        try {
            return quizRepository.findByContent(content);
        } catch (Exception e) {
            log.error("❌ 퀴즈 조회 실패 (contentId={}): {}", content != null ? content.getId() : null, e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ==========================================================
    // ✅ 4. 전체 퀴즈 조회
    // ==========================================================
    public List<Quiz> getAllQuizzes() {
        try {
            return quizRepository.findAll();
        } catch (Exception e) {
            log.error("❌ 전체 퀴즈 조회 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ==========================================================
    // ✅ 5. 로컬 채점 로직 (quizId 변환 안정화 버전)
    // ==========================================================
    public Map<String, Object> gradeQuizLocally(List<Quiz> quizzes, List<QuizGradeRequest.Answer> answers) {
        if (quizzes == null || quizzes.isEmpty() || answers == null || answers.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        try {
            List<Map<String, Object>> results = new ArrayList<>();
            int correctCount = 0;
            int totalScore = 0;
            int totalAnswered = answers.size();

            // ✅ 제출된 quiz_id 목록 안전 파싱
            List<Integer> answeredIds = new ArrayList<>();
            for (QuizGradeRequest.Answer a : answers) {
                Integer parsedId = safelyParseToInt(a.getQuiz_id());
                if (parsedId != null) answeredIds.add(parsedId);
            }

            // ✅ 해당 quizId만 필터링 (없으면 전체)
            List<Quiz> targetQuizzes = answeredIds.isEmpty()
                    ? quizzes
                    : quizzes.stream().filter(q -> answeredIds.contains(q.getQuizId())).toList();

            for (QuizGradeRequest.Answer answer : answers) {
                Integer quizId = safelyParseToInt(answer.getQuiz_id());
                String userAnswer = Optional.ofNullable(answer.getUser_answer()).orElse("").trim();

                // ✅ quizId로 매칭 (없으면 question으로 백업)
                Quiz matchedQuiz = null;
                if (quizId != null) {
                    matchedQuiz = targetQuizzes.stream()
                            .filter(q -> Objects.equals(q.getQuizId(), quizId))
                            .findFirst()
                            .orElse(null);
                } else if (answer.getQuestion() != null) {
                    matchedQuiz = targetQuizzes.stream()
                            .filter(q -> q.getQuestion().trim().equalsIgnoreCase(answer.getQuestion().trim()))
                            .findFirst()
                            .orElse(null);
                }

                if (matchedQuiz == null) continue;

                boolean isCorrect = matchedQuiz.getCorrectAnswer().trim().equalsIgnoreCase(userAnswer);
                int score = isCorrect ? (100 / totalAnswered) : 0;

                if (isCorrect) correctCount++;
                totalScore += score;

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("quiz_id", matchedQuiz.getQuizId());
                result.put("question", matchedQuiz.getQuestion());
                result.put("user_answer", userAnswer);
                result.put("correct_answer", matchedQuiz.getCorrectAnswer());
                result.put("explanation", matchedQuiz.getExplanation());
                result.put("is_correct", isCorrect);
                result.put("score", score);
                results.add(result);
            }

            Map<String, Object> resultBody = new LinkedHashMap<>();
            resultBody.put("final_total_score", totalScore);
            resultBody.put("correct_count", correctCount);
            resultBody.put("total_questions", totalAnswered);
            resultBody.put("results", results);

            return resultBody;

        } catch (Exception e) {
            log.error("❌ 채점 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // ✅ 6. quiz_attempts 저장 (예외 처리 포함)
    // ==========================================================
    public void saveQuizAttempt(Progress progress, Map<String, Object> result) {
        try {
            if (progress == null || result == null)
                throw new CustomException(ErrorCode.INVALID_INPUT);

            Float score = ((Number) result.get("final_total_score")).floatValue();
            Integer totalQuestions = (Integer) result.get("total_questions");
            Integer correctAnswers = (Integer) result.get("correct_count");

            QuizAttempt attempt = QuizAttempt.builder()
                    .progress(progress)
                    .score(score)
                    .totalQuestions(totalQuestions)
                    .correctAnswers(correctAnswers)
                    .build();

            quizAttemptRepository.save(attempt);
        } catch (DataAccessException e) {
            log.error("❌ DB 저장 실패 (QuizAttempt): {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        } catch (Exception e) {
            log.error("❌ QuizAttempt 저장 중 오류: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // ✅ 7. 안전한 quiz_id 변환 유틸
    // ==========================================================
    private Integer safelyParseToInt(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        } catch (NumberFormatException ignored) {
            log.warn("⚠️ quiz_id 변환 실패 (value={})", obj);
        }
        return null;
    }
}
