package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.enums.Difficulty;
import com.rookies4.MiniProject3.dto.QuizDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final ContentRepository contentRepository;
    private final QuizRepository quizRepository;
    private final AiService aiService;

    public List<Map<String, Object>> generateQuiz(Long contentId, QuizDto.Request request) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        try {
            Map<String, Object> response = aiService.generateQuizFromAI(
                    contentId, request.getCount(), request.getDifficulty()
            );

            // ✅ FastAPI의 응답 키 "questions"
            List<Map<String, Object>> quizList = (List<Map<String, Object>>) response.get("questions");
            if (quizList == null || quizList.isEmpty()) {
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }

            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Map<String, Object> quizData : quizList) {
                String question = (String) quizData.getOrDefault("question_text", "문제 없음");
                String options = (String) quizData.getOrDefault("options", "[]");
                String answer = (String) quizData.getOrDefault("answer", "정답 없음");
                String explanation = (String) quizData.getOrDefault("explanation", "");
                Difficulty diff = Difficulty.valueOf(request.getDifficulty());

                Quiz quiz = Quiz.builder()
                        .content(content)
                        .question(question)
                        .options(options)
                        .correctAnswer(answer)
                        .explanation(explanation)
                        .difficulty(diff)
                        .build();

                quizRepository.save(quiz);

                resultList.add(Map.of(
                        "quizId", quiz.getId(),
                        "question", quiz.getQuestion(),
                        "options", quiz.getOptions(),
                        "difficulty", quiz.getDifficulty().toString()
                ));
            }

            return resultList;

        } catch (Exception e) {
            log.error("[AI 퀴즈 생성 실패]", e);
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        }
    }
}
