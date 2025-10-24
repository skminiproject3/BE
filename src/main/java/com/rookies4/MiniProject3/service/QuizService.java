package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.dto.QuizGradeRequest;
import com.rookies4.MiniProject3.dto.QuizResponseDto;
import com.rookies4.MiniProject3.repository.QuizAttemptRepository;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ✅ 퀴즈 저장 (quiz_id 자동 생성)
     */
    public Quiz saveQuiz(Content content, String question, String correctAnswer, String optionsJson, String explanation) {
        // content 내 가장 마지막 quiz_id 조회 후 +1
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
    }

    /**
     * FastAPI에서 생성된 퀴즈 리스트 저장
     */
    public int saveGeneratedQuizzes(Content content, List<QuizResponseDto> quizzes) {
        int count = 0;
        for (QuizResponseDto dto : quizzes) {
            try {
                String optionsJson = objectMapper.writeValueAsString(dto.getOptions());
                saveQuiz(content, dto.getQuestion(), dto.getCorrectAnswer(), optionsJson, dto.getExplanation());
                count++;
            } catch (Exception e) {
                System.err.println("⚠️ 퀴즈 저장 실패: " + dto.getQuestion());
            }
        }
        return count;
    }

    /**
     * 콘텐츠별 퀴즈 조회
     */
    public List<Quiz> getQuizzesByContent(Content content) {
        return quizRepository.findByContent(content);
    }

    /**
     * 전체 퀴즈 조회
     */
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    /**
     * ✅ quiz_id 기반 로컬 채점 (question 없어도 동작)
     */
    public Map<String, Object> gradeQuizLocally(List<Quiz> quizzes, List<QuizGradeRequest.Answer> answers) {
        List<Map<String, Object>> results = new ArrayList<>();
        int correctCount = 0;
        int totalScore = 0;

        for (QuizGradeRequest.Answer answer : answers) {
            Long quizId = null;
            String question = answer.getQuestion();
            String userAnswer = answer.getUser_answer();

            // ✅ quiz_id 값이 있으면 먼저 사용
            try {
                if (answer.getQuiz_id() != null) {
                    quizId = Long.parseLong(answer.getQuiz_id().toString());
                }
            } catch (Exception ignored) {}

            final Long finalQuizId = quizId; // ✅ 람다에서 사용할 final 변수

            Quiz matchedQuiz = null;
            if (finalQuizId != null) {
                matchedQuiz = quizzes.stream()
                        .filter(q -> Objects.equals(q.getQuizId(), finalQuizId.intValue()))
                        .findFirst()
                        .orElse(null);
            } else if (question != null) {
                matchedQuiz = quizzes.stream()
                        .filter(q -> q.getQuestion().trim().equalsIgnoreCase(question.trim()))
                        .findFirst()
                        .orElse(null);
            }

            if (matchedQuiz == null) continue;

            boolean isCorrect = matchedQuiz.getCorrectAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            int score = isCorrect ? (100 / quizzes.size()) : 0;
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
        resultBody.put("total_questions", quizzes.size());
        resultBody.put("results", results);
        return resultBody;
    }

    /**
     * ✅ quiz_attempts 저장
     */
    public void saveQuizAttempt(Progress progress, Map<String, Object> result) {
        try {
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
        } catch (Exception e) {
            System.err.println("⚠️ QuizAttempt 저장 실패: " + e.getMessage());
        }
    }
}
