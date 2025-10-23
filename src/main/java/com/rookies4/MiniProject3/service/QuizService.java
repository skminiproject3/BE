package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.dto.QuizGradeRequest;
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

    /**
     * 퀴즈 저장 (Difficulty 없음)
     */
    public Quiz saveQuiz(Content content, String question, String correctAnswer, String optionsJson, String explanation) {
        Quiz quiz = Quiz.builder()
                .content(content)
                .question(question)
                .options(optionsJson)
                .correctAnswer(correctAnswer)
                .explanation(explanation)
                .build();
        return quizRepository.save(quiz);
    }

    /**
     * 콘텐츠별 퀴즈 조회
     */
    public List<Quiz> getQuizzesByContent(Content content) {
        return quizRepository.findByContent(content);
    }

    /**
     * 전체 퀴즈 조회 (관리용)
     */
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    /**
     * 로컬에서 퀴즈 채점 (FastAPI 호출 없이 DB 정답 비교)
     */
    public Map<String, Object> gradeQuizLocally(List<Quiz> quizzes, List<QuizGradeRequest.Answer> answers) {
        List<Map<String, Object>> results = new ArrayList<>();
        int correctCount = 0;
        int totalScore = 0;

        for (QuizGradeRequest.Answer answer : answers) {
            String question = answer.getQuestion();
            String userAnswer = answer.getUser_answer();

            Quiz matchedQuiz = quizzes.stream()
                    .filter(q -> q.getQuestion().trim().equalsIgnoreCase(question.trim()))
                    .findFirst()
                    .orElse(null);

            if (matchedQuiz == null) continue;

            boolean isCorrect = matchedQuiz.getCorrectAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            int score = isCorrect ? (100 / quizzes.size()) : 0;
            if (isCorrect) correctCount++;
            totalScore += score;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
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
     * 채점 결과를 quiz_attempts 테이블에 저장
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
