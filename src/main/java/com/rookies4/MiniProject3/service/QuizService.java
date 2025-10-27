package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.*;
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

/**
 * 퀴즈 생성 / 저장 / 채점 / 결과 관리 서비스
 * ✅ 회차(batch) 단위로 퀴즈 세트를 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================================
    // ✅ 1. 최신 batch 번호 조회
    // ==========================================================
    public int getLatestBatchForContent(Content content) {
        return quizRepository.findTopByContentOrderByQuizBatchDesc(content)
                .map(Quiz::getQuizBatch)
                .orElse(0);
    }

    // ==========================================================
    // ✅ 2. 단일 퀴즈 저장
    // ==========================================================
    private Quiz saveSingleQuiz(
            Content content,
            int quizId,
            int quizBatch,
            String question,
            String correctAnswer,
            String optionsJson,
            String explanation
    ) {
        try {
            Quiz quiz = Quiz.builder()
                    .content(content)
                    .quizId(quizId)
                    .quizBatch(quizBatch)
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
            log.error("❌ 예기치 못한 오류 (saveSingleQuiz): {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // ✅ 3. FastAPI 퀴즈 세트 저장 (새로운 batch 자동 생성)
    // ==========================================================
    public List<Quiz> saveGeneratedQuizSet(Content content, List<QuizResponseDto> quizzesFromLLM) {
        if (content == null)
            throw new CustomException(ErrorCode.CONTENT_NOT_FOUND);
        if (quizzesFromLLM == null || quizzesFromLLM.isEmpty())
            return Collections.emptyList();

        try {
            int newBatch = getLatestBatchForContent(content) + 1;

            int startQuizId = quizRepository.findTopByContentOrderByQuizIdDesc(content)
                    .map(q -> q.getQuizId() + 1)
                    .orElse(1);

            List<Quiz> savedList = new ArrayList<>();
            int runningQuizId = startQuizId;

            for (QuizResponseDto dto : quizzesFromLLM) {
                String optionsJson;
                try {
                    optionsJson = objectMapper.writeValueAsString(dto.getOptions());
                } catch (JsonProcessingException e) {
                    optionsJson = "[]";
                }

                Quiz saved = saveSingleQuiz(
                        content,
                        runningQuizId,
                        newBatch,
                        dto.getQuestion(),
                        dto.getCorrectAnswer(),
                        optionsJson,
                        dto.getExplanation()
                );
                savedList.add(saved);
                runningQuizId++;
            }

            log.info("✅ 퀴즈 세트 저장 완료 (contentId={}, batch={}, count={})",
                    content.getId(), newBatch, savedList.size());
            return savedList;

        } catch (Exception e) {
            log.error("❌ saveGeneratedQuizSet 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // ✅ 4. 콘텐츠 + 회차별 퀴즈 조회
    // ==========================================================
    public List<Quiz> getQuizzesByContentAndBatch(Content content, Integer batch) {
        try {
            return quizRepository.findByContentAndQuizBatch(content, batch);
        } catch (Exception e) {
            log.error("❌ 퀴즈 조회 실패 (contentId={}, batch={}): {}", content.getId(), batch, e.getMessage());
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ==========================================================
    // ✅ 5. 최신 회차 퀴즈 조회
    // ==========================================================
    public List<Quiz> getLatestBatchQuizzes(Content content) {
        int latestBatch = getLatestBatchForContent(content);
        if (latestBatch == 0) return Collections.emptyList();
        return getQuizzesByContentAndBatch(content, latestBatch);
    }

    // ==========================================================
    // ✅ 6. 로컬 채점 (회차 단위) — 개선된 정답 비교 로직 포함
    // ==========================================================
    public Map<String, Object> gradeQuizLocally(List<Quiz> quizzes, List<QuizGradeRequest.Answer> answers) {
        if (quizzes == null || quizzes.isEmpty() || answers == null || answers.isEmpty())
            throw new CustomException(ErrorCode.INVALID_INPUT);

        List<Map<String, Object>> results = new ArrayList<>();
        int correctCount = 0;
        int totalScore = 0;
        int totalAnswered = answers.size();

        for (QuizGradeRequest.Answer answer : answers) {
            Integer quizId = safelyParseToInt(answer.getQuiz_id());
            String userAnswer = Optional.ofNullable(answer.getUser_answer()).orElse("").trim();

            Quiz matchedQuiz = quizzes.stream()
                    .filter(q -> Objects.equals(q.getQuizId(), quizId))
                    .findFirst()
                    .orElse(null);

            if (matchedQuiz == null)
                continue;

            String correctAnswer = matchedQuiz.getCorrectAnswer().trim();
            boolean isCorrect = false;

            // 정답 비교 로직
            if (correctAnswer.equalsIgnoreCase(userAnswer)) {
                // 완전 일치 (대소문자 무시)
                isCorrect = true;
            } else if (correctAnswer.toUpperCase().startsWith(userAnswer.toUpperCase() + ".")) {
                // 예: "B" vs "B. 단순 치환 암호"
                isCorrect = true;
            } else if (userAnswer.length() == 1 && correctAnswer.toUpperCase().contains(userAnswer.toUpperCase() + ".")) {
                // 예: "b" vs "B. 단순 치환 암호"
                isCorrect = true;
            } else if (correctAnswer.replaceAll("[^ㄱ-ㅎ가-힣a-zA-Z0-9]", "")
                    .equalsIgnoreCase(userAnswer.replaceAll("[^ㄱ-ㅎ가-힣a-zA-Z0-9]", ""))) {
                // 예: "단순 치환 암호" vs "B. 단순 치환 암호" (기호 제거 후 비교)
                isCorrect = true;
            }

            int score = isCorrect ? (100 / totalAnswered) : 0;
            if (isCorrect) correctCount++;
            totalScore += score;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("quiz_id", matchedQuiz.getQuizId());
            result.put("question", matchedQuiz.getQuestion());
            result.put("user_answer", userAnswer);
            result.put("correct_answer", correctAnswer);
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
    }

    // ==========================================================
    // ✅ 7. quiz_attempts 저장 (batch 포함)
    // ==========================================================
    public void saveQuizAttempt(Progress progress, Map<String, Object> result, int batch) {
        try {

            Float score = 0f;
            Integer totalQuestions = 0;
            Integer correctAnswers = 0;

            if (result != null) {
                score = Optional.ofNullable((Number) result.get("final_total_score"))
                        .map(Number::floatValue).orElse(0f);
                totalQuestions = Optional.ofNullable((Integer) result.get("total_questions")).orElse(0);
                correctAnswers = Optional.ofNullable((Integer) result.get("correct_count")).orElse(0);
            }

            QuizAttempt attempt = QuizAttempt.builder()
                    .progress(progress)
                    .score(score)
                    .totalQuestions(totalQuestions)
                    .correctAnswers(correctAnswers)
                    .quizBatch(batch)
                    .build();

            quizAttemptRepository.save(attempt);

            log.info("🧾 QuizAttempt 저장 완료 | progress_id={} | batch={} | score={} | correct={}/{}",
                    progress.getId(), batch, score, correctAnswers, totalQuestions);

        } catch (Exception e) {
            log.error("❌ QuizAttempt 저장 중 오류: {}", e.getMessage(), e);
        }
    }

    // ==========================================================
    // ✅ 8. Progress별 시도 조회
    // ==========================================================
    public List<QuizAttempt> getAttemptsByProgress(Progress progress) {
        try {
            return quizAttemptRepository.findByProgress(progress);
        } catch (Exception e) {
            log.error("❌ QuizAttempt 조회 실패 (progress_id={}): {}", progress.getId(), e.getMessage());
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ==========================================================
    // ✅ 9. 안전한 quiz_id 파싱
    // ==========================================================
    private Integer safelyParseToInt(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
