package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

    // ---------- 1) 최신 batch ----------
    public int getLatestBatchForContent(Content content) {
        return quizRepository.findTopByContentOrderByQuizBatchDesc(content)
                .map(Quiz::getQuizBatch)
                .orElse(0);
    }

    // ---------- 2) 단일 퀴즈 저장 ----------
    private Quiz saveSingleQuiz(Content content, int quizId, int quizBatch,
                                String question, String correctAnswer, String optionsJson, String explanation) {
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

    // ---------- 3) 세트 저장 ----------
    public List<Quiz> saveGeneratedQuizSet(Content content, List<QuizResponseDto> quizzesFromLLM) {
        if (content == null) throw new CustomException(ErrorCode.CONTENT_NOT_FOUND);
        if (quizzesFromLLM == null || quizzesFromLLM.isEmpty()) return Collections.emptyList();

        try {
            int newBatch = getLatestBatchForContent(content) + 1;
            int startQuizId = quizRepository.findTopByContentOrderByQuizIdDesc(content)
                    .map(q -> q.getQuizId() + 1).orElse(1);

            List<Quiz> savedList = new ArrayList<>();
            int runningQuizId = startQuizId;

            for (QuizResponseDto dto : quizzesFromLLM) {
                String optionsJson;
                try {
                    optionsJson = objectMapper.writeValueAsString(dto.getOptions());
                } catch (Exception e) {
                    optionsJson = "[]";
                }
                Quiz saved = saveSingleQuiz(
                        content, runningQuizId, newBatch,
                        dto.getQuestion(), dto.getCorrectAnswer(), optionsJson, dto.getExplanation()
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

    // ---------- 4) 조회 ----------
    public List<Quiz> getQuizzesByContentAndBatch(Content content, Integer batch) {
        try {
            return quizRepository.findByContentAndQuizBatch(content, batch);
        } catch (Exception e) {
            log.error("❌ 퀴즈 조회 실패 (contentId={}, batch={}): {}", content.getId(), batch, e.getMessage());
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    public List<Quiz> getLatestBatchQuizzes(Content content) {
        int latestBatch = getLatestBatchForContent(content);
        if (latestBatch == 0) return Collections.emptyList();
        return getQuizzesByContentAndBatch(content, latestBatch);
    }

    // ---------- 5) 로컬 채점 ----------
    public Map<String, Object> gradeQuizLocally(List<Quiz> quizzes, List<QuizGradeRequest.Answer> answers) {
        if (quizzes == null || quizzes.isEmpty() || answers == null || answers.isEmpty())
            throw new CustomException(ErrorCode.INVALID_INPUT);

        Map<Integer, String> userAnsMap = new HashMap<>();
        for (QuizGradeRequest.Answer a : answers) {
            Integer qid = safelyParseToInt(a.getQuiz_id());
            if (qid != null) userAnsMap.put(qid, Optional.ofNullable(a.getUser_answer()).orElse("").trim());
        }

        int totalQuestions = quizzes.size();
        int correctCount = 0;
        List<Map<String, Object>> items = new ArrayList<>();

        for (Quiz q : quizzes) {
            Integer qid = q.getQuizId();
            String userRaw = userAnsMap.getOrDefault(qid, "");

            List<String> options = parseOptions(q.getOptions());
            String correctText = normalizeToOptionText(q.getCorrectAnswer(), options);
            String userText    = normalizeToOptionText(userRaw, options);

            boolean isCorrect = !userText.isBlank()
                    && correctText.replaceAll("\\s+", "").equalsIgnoreCase(userText.replaceAll("\\s+", ""));
            if (isCorrect) correctCount++;

            Map<String, Object> one = new LinkedHashMap<>();
            one.put("quiz_id", qid);
            one.put("question", q.getQuestion());
            one.put("options", options);
            one.put("correct_answer", correctText);
            one.put("user_answer", userText);
            one.put("is_correct", isCorrect);
            one.put("score", isCorrect ? 1 : 0);
            one.put("explanation", q.getExplanation());
            items.add(one);
        }

        int finalScorePct = (totalQuestions > 0)
                ? (int) Math.round((correctCount * 100.0) / totalQuestions)
                : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("final_total_score", finalScorePct);
        result.put("correct_count", correctCount);
        result.put("total_questions", totalQuestions);
        result.put("results", items);
        return result;
    }

    // ---------- 6) 시도 저장 (요약값만 저장 / items 저장 안 함)  ※반환형 변경
    public QuizAttempt saveQuizAttempt(Progress progress, Map<String, Object> result, int batch) {
        try {
            Number s = (Number) result.getOrDefault("final_total_score", 0);
            Number t = (Number) result.getOrDefault("total_questions", 0);
            Number c = (Number) result.getOrDefault("correct_count", 0);

            QuizAttempt attempt = QuizAttempt.builder()
                    .progress(progress)
                    .score(s.floatValue())
                    .totalQuestions(t.intValue())
                    .correctAnswers(c.intValue())
                    .quizBatch(batch)
                    .build();

            QuizAttempt saved = quizAttemptRepository.save(attempt);
            log.info("🧾 QuizAttempt 저장 완료 | progress_id={} | batch={} | attempt_id={} | score={} | correct={}/{}",
                    progress.getId(), batch, saved.getId(), saved.getScore(), saved.getCorrectAnswers(), saved.getTotalQuestions());
            return saved;

        } catch (Exception e) {
            log.error("❌ QuizAttempt 저장 중 오류", e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ---------- 7) Progress별 시도 조회 ----------
    public List<QuizAttempt> getAttemptsByProgress(Progress progress) {
        try {
            return quizAttemptRepository.findByProgress(progress);
        } catch (Exception e) {
            log.error("❌ QuizAttempt 조회 실패 (progress_id={}): {}", progress.getId(), e.getMessage());
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ---------- 8) 특정 시도 조회(요약) → Dashboard에서 사용 ----------
    public Optional<QuizAttempt> getAttemptById(Long attemptId) {
        return quizAttemptRepository.findById(attemptId);
    }

    // ---------- 9) 컨트롤러에서 바로 쓰는 "채점→저장→응답" ----------
    // ※ DB 저장은 요약만, 문항 results는 "응답으로만" 전달(재조회/저장은 안 함)
    public Map<String, Object> gradeAndSave(Content content, Progress progress, Integer batchOrNull, List<QuizGradeRequest.Answer> answers) {
        if (content == null) throw new CustomException(ErrorCode.CONTENT_NOT_FOUND);

        int batch = (batchOrNull != null) ? batchOrNull : getLatestBatchForContent(content);
        List<Quiz> quizzes = getQuizzesByContentAndBatch(content, batch);
        if (quizzes.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("attempt_id", null);
            empty.put("content_id", content.getId());
            empty.put("batch", batch);
            empty.put("final_total_score", 0);
            empty.put("correct_count", 0);
            empty.put("total_questions", 0);
            empty.put("results", Collections.emptyList());
            return empty;
        }

        Map<String, Object> graded = gradeQuizLocally(quizzes, answers);
        QuizAttempt attempt = saveQuizAttempt(progress, graded, batch);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("attempt_id", attempt.getId());
        resp.put("content_id", content.getId());
        resp.put("batch", batch);
        resp.put("final_total_score", graded.get("final_total_score"));
        resp.put("correct_count", graded.get("correct_count"));
        resp.put("total_questions", graded.get("total_questions"));
        resp.put("results", graded.get("results")); // <-- 문항은 응답으로만
        return resp;
    }

    // ---------- 헬퍼 ----------
    private Integer safelyParseToInt(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            if (optionsJson.contains("|")) {
                return Arrays.stream(optionsJson.split("\\|"))
                        .map(String::trim).filter(s -> !s.isBlank()).toList();
            }
            log.warn("options 파싱 실패: {}", optionsJson, e);
            return List.of();
        }
    }

    private String normalizeToOptionText(String raw, List<String> options) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "";

        if (s.length() == 1 && Character.isLetter(s.charAt(0))) {
            int idx = Character.toUpperCase(s.charAt(0)) - 'A';
            return (idx >= 0 && idx < options.size()) ? options.get(idx).trim() : "";
        }
        if (s.matches("^\\d+$")) {
            int n = Integer.parseInt(s);
            int idx = (n < options.size()) ? n : n - 1;
            return (idx >= 0 && idx < options.size()) ? options.get(idx).trim() : "";
        }

        String noPrefix = s.replaceAll("^[A-Za-z]\\s*\\.|^\\d+\\s*\\.", "").trim();
        String norm = noPrefix.replaceAll("\\s+", "").toLowerCase();
        for (String opt : options) {
            String o = (opt == null ? "" : opt).trim();
            if (norm.equals(o.replaceAll("\\s+", "").toLowerCase())) {
                return o;
            }
        }
        return noPrefix;
    }
}
