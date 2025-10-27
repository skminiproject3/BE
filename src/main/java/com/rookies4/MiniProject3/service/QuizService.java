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
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 접두 "A. " / "1. " 제거용
    private static final Pattern LETTER_PREFIX = Pattern.compile("^[A-F]\\.?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUM_PREFIX    = Pattern.compile("^\\d+\\.?\\s*");

    // ========== 1) 최신 batch ==========
    public int getLatestBatchForContent(Content content) {
        return quizRepository.findTopByContentOrderByQuizBatchDesc(content)
                .map(Quiz::getQuizBatch)
                .orElse(0);
    }

    // ========== 2) 단일 퀴즈 저장 ==========
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

    // ========== 3) 세트 저장 ==========
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

    // ========== 4) 조회 ==========
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

    // ========== 5) 채점 (DTO: quiz_id + user_answer 만 사용) ==========
    // ---------- 5) 로컬 채점 (관대한 정규화) ----------
    public Map<String, Object> gradeQuizLocally(List<Quiz> quizzes, List<QuizGradeRequest.Answer> answers) {
        if (quizzes == null || quizzes.isEmpty() || answers == null || answers.isEmpty())
            throw new CustomException(ErrorCode.INVALID_INPUT);

        // 사용자 답안을 {quizId -> raw}로 준비
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
            String userRaw = userAnsMap.getOrDefault(qid, "");          // 예: "B. 56비트"

            List<String> options = parseOptions(q.getOptions());         // ["A. ...", "B. ...", ...]
            String corrRaw = Optional.ofNullable(q.getCorrectAnswer()).orElse(""); // 예: "B. 56비트"
            String corrLetter = firstLetter(corrRaw);                    // "B"
            String corrText   = stripPrefix(corrRaw);                    // "56비트"

            // 사용자도 같은 방식으로 파생값 준비
            String userLetter = firstLetter(userRaw);                    // "B" 또는 ""
            String userText   = stripPrefix(userRaw);                    // "56비트" 또는 ""

            // 옵션 정규화(접두 제거 → 기호/공백 제거 → lower)
            List<String> optNorms = new ArrayList<>();
            for (String opt : options) optNorms.add(normalizeForCompare(stripPrefix(opt)));

            String corrNorm = normalizeForCompare(corrText);
            String userNorm = normalizeForCompare(userText);
            String userNormFromRaw = normalizeForCompare(stripPrefix(userRaw));

            // 옵션에서 정답 인덱스 추정
            int corrIdx = letterToIndex0(corrLetter);  // B -> 1
            int userIdx = letterToIndex0(userLetter);  // B -> 1 (사용자가 letter 보냈다면)

            boolean ok = false;

            // (1) letter 일치
            if (!ok && !corrLetter.isEmpty() && corrLetter.equalsIgnoreCase(userLetter)) ok = true;

            // (2) 인덱스 일치
            if (!ok && corrIdx >= 0 && corrIdx < optNorms.size() && userIdx == corrIdx) ok = true;

            // (3) 원문 전체(정규화) 일치
            if (!ok && !userRaw.isBlank() && !corrRaw.isBlank() &&
                    normalizeForCompare(corrRaw).equals(normalizeForCompare(userRaw))) ok = true;

            // (4) 접두 제거 텍스트 정규화 일치
            if (!ok && !userNorm.isBlank() && !corrNorm.isBlank() && userNorm.equals(corrNorm)) ok = true;

            // (5) 사용자 정규화가 옵션의 것과 매칭되고, 그 옵션이 정답 옵션일 때
            if (!ok) {
                int userOptIdx = indexOfNormalized(optNorms, userNormFromRaw);
                if (userOptIdx < 0) userOptIdx = indexOfNormalized(optNorms, userNorm);
                ok = (userOptIdx >= 0 && userOptIdx == corrIdx) ||
                        (userOptIdx >= 0 && !corrNorm.isBlank() && optNorms.get(userOptIdx).equals(corrNorm));
            }

            if (ok) correctCount++;

            // ← 문항별 디버그
            log.debug("GRADE qid={} | corr='{}'({}) | user='{}'({}) | ok={}",
                    qid, corrRaw, corrLetter, userRaw, userLetter, ok);

            Map<String, Object> one = new LinkedHashMap<>();
            one.put("quiz_id", qid);
            one.put("question", q.getQuestion());
            one.put("options", options);
            one.put("correct_answer", corrRaw);
            one.put("user_answer", userRaw);
            one.put("is_correct", ok);
            one.put("score", ok ? 1 : 0);
            one.put("explanation", q.getExplanation());
            items.add(one);
        }

        int finalScorePct = (totalQuestions > 0)
                ? (int) Math.round((correctCount * 100.0) / totalQuestions * 100.0 / 100.0)
                : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("final_total_score", finalScorePct);
        result.put("correct_count", correctCount);
        result.put("total_questions", totalQuestions);
        result.put("results", items);
        return result;
    }

    /* ====== 보조 함수들 (같은 클래스 안) ====== */

    // "A. " / "1. " 같은 접두 제거
    private String stripPrefix(String s) {
        if (s == null) return "";
        return s.replaceFirst("^[A-Za-z]\\s*\\.|^\\d+\\s*\\.", "").trim();
    }

    // 첫 글자가 알파벳이면 대문자로 반환
    private String firstLetter(String s) {
        if (s == null || s.isBlank()) return "";
        char c = s.trim().charAt(0);
        if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) return String.valueOf(Character.toUpperCase(c));
        return "";
    }

    // A→0, B→1 ...
    private int letterToIndex0(String letter) {
        if (letter == null || letter.isBlank()) return -1;
        char c = Character.toUpperCase(letter.charAt(0));
        if (c < 'A' || c > 'Z') return -1;
        return c - 'A';
    }

    // 비교용 정규화: 공백/점/중간점/쉼표/콜론 제거 + 소문자
    private String normalizeForCompare(String s) {
        if (s == null) return "";
        String t = s;
        t = t.replaceAll("\\s*([()\\[\\]{}])\\s*", "$1"); // 괄호 주변 공백 제거
        t = t.replaceAll("[·•∙·.,:;]+", "");             // 구분기호 제거
        t = t.replaceAll("\\s+", "");                    // 모든 공백 제거
        return t.toLowerCase();
    }

    private int indexOfNormalized(List<String> norms, String targetNorm) {
        if (targetNorm == null || targetNorm.isBlank()) return -1;
        for (int i = 0; i < norms.size(); i++) if (targetNorm.equals(norms.get(i))) return i;
        return -1;
    }



    // ========== 6) 시도 저장 ==========
    public QuizAttempt saveQuizAttempt(Progress progress, Map<String, Object> result, int batch) {
        if (progress == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            Number sNum = toNumber(result.getOrDefault("final_total_score", 0));
            Number tNum = toNumber(result.getOrDefault("total_questions", 0));
            Number cNum = toNumber(result.getOrDefault("correct_count", 0));

            float score = sNum == null ? 0f : sNum.floatValue();
            int total   = tNum == null ? 0  : tNum.intValue();
            int correct = cNum == null ? 0  : cNum.intValue();

            QuizAttempt attempt = QuizAttempt.builder()
                    .progress(progress)
                    .score(score)
                    .totalQuestions(total)
                    .correctAnswers(correct)
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

    /** Map 꺼낸 값을 안전하게 Number로 변환 */
    private Number toNumber(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return (Number) v;
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            if (s.matches("^-?\\d+$")) return Integer.parseInt(s);
            if (s.matches("^-?\\d+(\\.\\d+)?$")) return Float.parseFloat(s);
        } catch (Exception ignored) {}
        return null;
    }

    // ========== 7) Progress별 시도 조회 ==========
    public List<QuizAttempt> getAttemptsByProgress(Progress progress) {
        try {
            return quizAttemptRepository.findByProgress(progress);
        } catch (Exception e) {
            log.error("❌ QuizAttempt 조회 실패 (progress_id={}): {}", progress.getId(), e.getMessage());
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    // ========== 8) 특정 시도 조회 ==========
    public Optional<QuizAttempt> getAttemptById(Long attemptId) {
        return quizAttemptRepository.findById(attemptId);
    }

    // ========== 9) 채점→저장→응답 ==========
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
        resp.put("results", graded.get("results"));
        return resp;
    }

    // ====== helpers ======
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

    // "A", "A. 56비트", "56비트", "1. 56비트", "2" 등 → 보기 텍스트로 정규화
    private String normalizeToOptionText(String raw, List<String> options) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "";

        // 1) 순수 문자(A/B/...)만 온 경우 → 인덱스 변환
        if (s.matches("^[A-Fa-f]$")) {
            int idx = Character.toUpperCase(s.charAt(0)) - 'A';
            return (idx >= 0 && idx < options.size()) ? clean(options.get(idx)) : "";
        }

        // 2) "A. ..." / "1. ..." 같은 접두 제거
        String noPrefix = LETTER_PREFIX.matcher(s).replaceFirst("");
        noPrefix = NUM_PREFIX.matcher(noPrefix).replaceFirst("").trim();

        // 3) 숫자만 온 경우(0/1-base 모두 허용)
        if (s.matches("^\\d+$")) {
            int n = Integer.parseInt(s);
            int idx0 = n;       // 0-base 가정
            int idx1 = n - 1;   // 1-base 가정
            if (idx0 >= 0 && idx0 < options.size()) return clean(options.get(idx0));
            if (idx1 >= 0 && idx1 < options.size()) return clean(options.get(idx1));
        }

        // 4) 보기와 공백무시 비교로 매칭
        String norm = compact(noPrefix);
        for (String opt : options) {
            if (compact(opt).equalsIgnoreCase(norm)) return clean(opt);
        }

        // 5) 매칭 실패 시 접두만 제거한 문자열 반환
        return noPrefix;
    }

    private boolean equalIgnoringSpaces(String a, String b) {
        return compact(a).equalsIgnoreCase(compact(b));
    }
    private String compact(String s) {
        return (s == null) ? "" : s.replaceAll("\\s+", "");
    }
    private String clean(String s) {
        return (s == null) ? "" : s.trim();
    }
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
