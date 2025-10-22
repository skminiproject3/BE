package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final ContentRepository contentRepository;
    private final QuizRepository quizRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ✅ FastAPI 퀴즈 생성 및 DB 저장
     */
    public List<Map<String, Object>> generateQuiz(Long contentId, QuizDto.Request request) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        try {
            Map<String, Object> response = aiService.generateQuizFromAI(
                    contentId, request.getCount(), request.getDifficulty()
            );

            List<Map<String, Object>> quizList = (List<Map<String, Object>>) response.get("questions");
            if (quizList == null || quizList.isEmpty()) {
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }

            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Map<String, Object> quizData : quizList) {
                String rawQuestion = (String) quizData.getOrDefault("question_text", "문제 없음");

                // ✅ answer / explanation 여러 키명 대응
                String answer = Optional.ofNullable((String) quizData.get("answer"))
                        .orElse((String) quizData.getOrDefault("correct_answer",
                                quizData.getOrDefault("correctAnswer", null)));

                String explanation = Optional.ofNullable((String) quizData.get("explanation"))
                        .orElse((String) quizData.getOrDefault("explain", ""));

                // ✅ 정답 누락 시 자동 추출
                if (answer == null || answer.isBlank() || answer.equalsIgnoreCase("정답 없음")) {
                    Matcher m1 = Pattern.compile("(정답|답)[:\\s]*([0-9])").matcher(rawQuestion);
                    Matcher m2 = Pattern.compile("---.*?([0-9])").matcher(rawQuestion);

                    if (m1.find()) {
                        answer = m1.group(2);
                        log.info("✅ [AI 누락 보정] 문제 내 '정답:' 패턴에서 추출됨 → {}", answer);
                    } else if (m2.find()) {
                        answer = m2.group(1);
                        log.info("✅ [AI 누락 보정] --- 이후에서 정답 추출됨 → {}", answer);
                    } else {
                        answer = "1"; // 기본값
                        log.warn("⚠️ [AI 정답 누락] 기본값 1번으로 설정");
                    }
                }

                // ✅ 보기 추출
                List<String> optionsList = extractOptions(rawQuestion);
                String questionOnly = cleanQuestion(rawQuestion);
                String optionsJson = "[]";

                if (!optionsList.isEmpty()) {
                    try {
                        List<String> numbered = new ArrayList<>();
                        for (int i = 0; i < optionsList.size(); i++) {
                            numbered.add((i + 1) + ". " + optionsList.get(i));
                        }
                        optionsJson = objectMapper.writeValueAsString(numbered);
                    } catch (Exception e) {
                        log.error("❌ 옵션 JSON 직렬화 실패", e);
                    }
                }

                Difficulty diff = Difficulty.valueOf(request.getDifficulty());

                // ✅ DB 저장
                Quiz quiz = Quiz.builder()
                        .content(content)
                        .question(questionOnly)
                        .options(optionsJson)
                        .correctAnswer(answer)
                        .explanation(explanation)
                        .difficulty(diff)
                        .build();

                quizRepository.save(quiz);

                // ✅ 응답 포맷 구성
                Map<String, Object> quizMap = new LinkedHashMap<>();
                quizMap.put("quizId", quiz.getId());
                quizMap.put("question", quiz.getQuestion());
                quizMap.put("difficulty", quiz.getDifficulty().toString());

                if (!optionsList.isEmpty()) {
                    quizMap.put("options", parseJsonOptions(optionsJson));
                }

                resultList.add(quizMap);
            }

            return resultList;

        } catch (Exception e) {
            log.error("[AI 퀴즈 생성 실패]", e);
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        }
    }

    /**
     * ✅ 보기 추출 (a/b/c/d 또는 1/2/3/4 형식 모두 인식)
     */
    private List<String> extractOptions(String text) {
        List<String> options = new ArrayList<>();

        // a) 또는 1. 형태 모두 인식
        Pattern pattern = Pattern.compile("(?m)^[ \\t]*([a-dA-D1-9])[)\\.\\s]+(.+)$");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String option = matcher.group(2).trim().replaceAll("\\s+", " ");
            if (option.length() > 1) {
                options.add(option);
            }
        }

        if (options.size() < 2) return Collections.emptyList();
        return options;
    }

    /**
     * ✅ 문제문만 추출 (보기 제거)
     */
    private String cleanQuestion(String text) {
        if (Pattern.compile("(?m)^[ \\t]*([a-dA-D1-9])[)\\.]").matcher(text).find()) {
            return text.split("(?m)^[ \\t]*([a-dA-D1-9])[)\\.]")[0].trim();
        }
        return text.trim();
    }

    /**
     * ✅ JSON → List 변환
     */
    private List<String> parseJsonOptions(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}

