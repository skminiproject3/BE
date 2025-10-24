package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.dto.QuizGradeRequest;
import com.rookies4.MiniProject3.dto.QuizResponseDto;
import com.rookies4.MiniProject3.dto.SummaryDto;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PythonServerClient {

    private final WebClient webClient;
    private final Dotenv dotenv = Dotenv.load();
    private final String openaiApiKey = dotenv.get("OPENAI_API_KEY");

    // ======================================
    // PDF 업로드 요청
    // ======================================
    public List<String> uploadPDFs(List<File> files) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (File f : files) body.add("files", new FileSystemResource(f));

        Map<String, Object> response = webClient.post()
                .uri("/upload_pdfs/")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("pdf_paths")) {
            return (List<String>) response.get("pdf_paths");
        }
        return Collections.emptyList();
    }

    // ======================================
    // 전체 요약 요청
    // ======================================
    public String summarize(List<String> pdfPaths) {
        Map<String, Object> body = Map.of("pdf_paths", pdfPaths);
        Map<String, Object> response = webClient.post()
                .uri("/summarize/full")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response != null ? (String) response.get("summaryText") : "요약 실패";
    }

    // ======================================
    // 단원별 요약 요청
    // ======================================
    public List<SummaryDto.Response> summarizeChapters(List<String> pdfPaths, String chapterRequest) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pdf_paths", pdfPaths);
            if (chapterRequest != null && !chapterRequest.isEmpty()) {
                body.put("chapter_request", chapterRequest);
            }

            SummaryDto.ChapterSummaryResponse response = webClient.post()
                    .uri("/summarize/chapter")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(SummaryDto.ChapterSummaryResponse.class)
                    .block();

            if (response != null && response.getSummaries() != null) {
                return response.getSummaries();
            } else {
                log.error("❌ Python 서버 단원별 요약 응답이 올바르지 않습니다.");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("🚨 단원별 요약 요청 실패", e);
            return Collections.emptyList();
        }
    }

    // ======================================
    // 질문 응답 요청
    // ======================================
    public String answerQuestion(String question, List<String> pdfPaths) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("question", question);
            body.put("pdf_paths", pdfPaths);

            Map<String, Object> response = webClient.post()
                    .uri("/question/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("answer")) {
                return (String) response.get("answer");
            } else {
                log.error("❌ Python 서버 질문 응답이 올바르지 않습니다.");
                return "질문 실패";
            }
        } catch (Exception e) {
            log.error("🚨 질문 요청 실패", e);
            return "질문 실패";
        }
    }

    // ======================================
    // LLM 보완
    // ======================================
    private QuizResponseDto enrichWithLLM(QuizResponseDto quiz) {
        try {
            if (quiz == null) return null;

            boolean needsFix = quiz.getCorrectAnswer() == null ||
                    quiz.getCorrectAnswer().equals("정답 정보 없음") ||
                    quiz.getExplanation() == null ||
                    quiz.getExplanation().equals("해설 정보 없음");

            if (!needsFix) return quiz;

            String prompt = String.format("""
                문제: %s
                보기: %s

                보기 중 올바른 정답과 이유를 아래 JSON 형식으로 작성하세요.
                {
                    "correct_answer": "(정답 전체 문장)",
                    "explanation": "(정답 이유 한 줄)"
                }
            """, quiz.getQuestion(), String.join(", ", quiz.getOptions()));

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            Map<String, Object> response = webClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                Map<String, Object> choice = ((List<Map<String, Object>>) response.get("choices")).get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                String content = message.get("content").toString().trim();
                content = content.replace("```json", "").replace("```", "").trim();

                Map<String, Object> parsed = new ObjectMapper().readValue(content, Map.class);
                quiz.setCorrectAnswer(parsed.getOrDefault("correct_answer", "정답 정보 없음").toString());
                quiz.setExplanation(parsed.getOrDefault("explanation", "해설 정보 없음").toString());
            }
        } catch (Exception e) {
            log.error("⚠️ LLM 보완 오류: {}", e.getMessage());
        }
        return quiz;
    }

    // ======================================
    // ✅ 퀴즈 생성 (FastAPI 경로 수정 완료)
    // ======================================
    public List<QuizResponseDto> generateQuiz(Long contentId, List<String> pdfPaths, int numQuestions, String difficulty) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pdf_paths", pdfPaths);
            body.put("num_questions", numQuestions);
            body.put("difficulty", difficulty);

            Object responseObj = webClient.post()
                    .uri("/api/contents/{contentId}/quiz/generate", contentId)  // ✅ FastAPI 경로 일치
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            if (responseObj == null) {
                log.error("Python 서버 응답이 null입니다.");
                return Collections.emptyList();
            }

            List<QuizResponseDto> quizzes = new ArrayList<>();

            if (responseObj instanceof List<?>) {
                quizzes = castToMapList(responseObj).stream()
                        .map(this::convertToQuizDto)
                        .collect(Collectors.toList());
            } else if (responseObj instanceof Map<?, ?> map && map.containsKey("questions")) {
                Object qObj = map.get("questions");
                if (qObj instanceof List<?>) {
                    quizzes = castToMapList(qObj).stream()
                            .map(this::convertToQuizDto)
                            .collect(Collectors.toList());
                }
            }

            return quizzes.stream()
                    .map(this::enrichWithLLM)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 퀴즈 생성 요청 실패", e);
            return Collections.emptyList();
        }
    }

    private QuizResponseDto convertToQuizDto(Map<String, Object> item) {
        try {
            String question = "";
            List<String> options = new ArrayList<>();
            String correct = "";
            String explanation = "";

            if (item.containsKey("question"))
                question = safeToString(item.get("question"));
            else if (item.containsKey("question_text"))
                question = safeToString(item.get("question_text"));
            else if (item.containsKey("quiz_text"))
                question = safeToString(item.get("quiz_text"));

            question = Arrays.stream(question.split("\n"))
                    .filter(line -> !line.matches("^[a-dA-D]\\).*"))
                    .map(String::trim)
                    .collect(Collectors.joining(" "))
                    .replaceAll("---", "")
                    .trim();

            if (item.containsKey("options"))
                options = parseOptions(item.get("options"));
            else if (item.containsKey("question_text")) {
                String qText = safeToString(item.get("question_text"));
                options = Arrays.stream(qText.split("\n"))
                        .filter(l -> l.matches("^[a-dA-D]\\).*"))
                        .map(String::trim)
                        .collect(Collectors.toList());
            }

            correct = safeToString(item.get("correct_answer"));
            explanation = safeToString(item.get("explanation"));

            if (question.isBlank()) question = "문제 정보 없음";
            if (options.isEmpty()) options = List.of("보기1", "보기2", "보기3", "보기4");

            return QuizResponseDto.builder()
                    .question(question)
                    .options(options)
                    .correctAnswer(correct.isBlank() ? "정답 정보 없음" : correct)
                    .explanation(explanation.isBlank() ? "해설 정보 없음" : explanation)
                    .build();

        } catch (Exception e) {
            log.error("⚠️ Quiz DTO 변환 실패: {}", item, e);
            return null;
        }
    }

    private List<String> parseOptions(Object obj) {
        try {
            if (obj instanceof List<?>) {
                return ((List<?>) obj).stream().map(Object::toString).collect(Collectors.toList());
            } else if (obj instanceof Map<?, ?> map) {
                return map.values().stream().map(Object::toString).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("⚠️ options 파싱 실패: {}", obj);
        }
        return List.of("보기1", "보기2", "보기3", "보기4");
    }

    private List<Map<String, Object>> castToMapList(Object obj) {
        try {
            return (List<Map<String, Object>>) obj;
        } catch (Exception e) {
            log.error("⚠️ Map 리스트 변환 실패: {}", obj, e);
            return Collections.emptyList();
        }
    }

    private String safeToString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    // ======================================
    // ✅ FastAPI 호환 채점 요청
    // ======================================
    public Map<String, Object> gradeQuiz(List<String> pdfPaths, List<QuizGradeRequest.Answer> answers) {
        try {
            List<String> normalizedPaths = pdfPaths.stream()
                    .filter(Objects::nonNull)
                    .map(p -> p.replace("\\", "/"))
                    .collect(Collectors.toList());

            List<Map<String, Object>> validAnswers = answers.stream()
                    .filter(a -> a.getQuestion() != null && a.getUser_answer() != null)
                    .map(a -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("question", a.getQuestion());
                        map.put("user_answer", a.getUser_answer());
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pdf_paths", normalizedPaths);
            body.put("answers", validAnswers);

            log.info("📤 [FastAPI 채점 요청 바디] {}", new ObjectMapper().writeValueAsString(body));

            Map<String, Object> response = webClient.post()
                    .uri("/quiz/grade")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return Map.of("message", "채점 실패: 응답 없음");
            }

            log.info("✅ [FastAPI 채점 결과 수신] {}", new ObjectMapper().writeValueAsString(response));

            List<Map<String, Object>> results = new ArrayList<>();
            int correctCount = 0;
            int totalScore = 0;

            Object rawResults = response.get("results");
            if (rawResults instanceof List<?>) {
                for (Object obj : (List<?>) rawResults) {
                    Map<String, Object> r = (Map<String, Object>) obj;
                    results.add(r);
                    if (Boolean.TRUE.equals(r.get("is_correct"))) correctCount++;
                    if (r.get("score") instanceof Number num) totalScore += num.intValue();
                }
            }

            int totalQuestions = results.size();
            int finalScore = response.containsKey("final_total_score")
                    ? ((Number) response.get("final_total_score")).intValue()
                    : totalScore;

            Map<String, Object> resultBody = new LinkedHashMap<>();
            resultBody.put("final_total_score", finalScore);
            resultBody.put("correct_count", correctCount);
            resultBody.put("total_questions", totalQuestions);
            resultBody.put("results", results);

            return resultBody;

        } catch (Exception e) {
            log.error("🚨 채점 요청 중 오류 발생", e);
            return Map.of("message", "채점 중 오류 발생");
        }
    }
}
