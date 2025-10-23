package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.dto.*;
import com.rookies4.MiniProject3.dto.SummaryDto.Response;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    @Autowired
    private final ContentService contentService;

    @Autowired
    private final PythonServerClient pythonClient;

    @Autowired
    private final QuizService quizService; // ✅ 퀴즈 저장 서비스 추가

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ======================================
    // 문서 업로드 및 DB 저장
    // ======================================
    @PostMapping("/upload")
    public ResponseEntity<List<ContentDto.UploadResponse>> uploadContents(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("title") String title) {

        List<File> savedFiles = new ArrayList<>();
        List<ContentDto.UploadResponse> responses = new ArrayList<>();
        String saveDir = "C:/uploads/";
        new File(saveDir).mkdirs();

        for (MultipartFile file : files) {
            try {
                String uuidFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                File savedFile = new File(saveDir + uuidFileName);
                file.transferTo(savedFile);
                savedFiles.add(savedFile);

                ContentDto.UploadResponse response = contentService.saveToDb(
                        savedFile.getAbsolutePath(), file.getOriginalFilename(), title, 1L
                );
                responses.add(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Python 서버에도 업로드 요청
        pythonClient.uploadPDFs(savedFiles);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responses);
    }

    // ======================================
    // 콘텐츠 상태 조회
    // ======================================
    @GetMapping("/{contentId}/status")
    public ResponseEntity<ContentDto.StatusResponse> getContentStatus(@PathVariable Long contentId) {
        ContentDto.StatusResponse response = contentService.getContentStatus(contentId);
        return ResponseEntity.ok(response);
    }

    // ======================================
    // 전체 요약 요청
    // ======================================
    @PostMapping("/{contentId}/summarize")
    public ResponseEntity<String> summarizeContent(@PathVariable Long contentId) {
        List<String> pdfPaths = contentService.getPdfPaths(contentId);
        String summary = pythonClient.summarize(pdfPaths);
        return ResponseEntity.ok(summary);
    }

    // ======================================
    // 단원별 요약 요청
    // ======================================
    @PostMapping("/{contentId}/summaries")
    public ResponseEntity<List<Response>> getSummaries(
            @PathVariable Long contentId,
            @RequestBody SummaryChapterRequest request) {

        List<String> pdfPaths = contentService.getPdfPaths(contentId);
        List<Response> summaries = pythonClient.summarizeChapters(pdfPaths, request.getChapterRequest());
        return ResponseEntity.ok(summaries);
    }

    // ======================================
    // 질문 응답 요청
    // ======================================
    @PostMapping("/{contentId}/ask")
    public ResponseEntity<String> askQuestion(
            @PathVariable Long contentId,
            @RequestParam("question") String question) {

        List<String> pdfPaths = contentService.getPdfPaths(contentId);
        String answer = pythonClient.answerQuestion(question, pdfPaths);
        return ResponseEntity.ok(answer);
    }

    // ======================================
    // 연습문제 생성 (AI + DB 자동 저장)
    // ======================================
    @PostMapping("/{contentId}/quiz/generate")
    public ResponseEntity<List<QuizResponseDto>> generateQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizRequest request) {

        // 1️⃣ 해당 콘텐츠의 PDF 경로 가져오기
        List<String> pdfPaths = contentService.getPdfPaths(contentId);

        // 2️⃣ Python 서버에서 퀴즈 생성 요청
        List<QuizResponseDto> quizList = pythonClient.generateQuiz(
                pdfPaths,
                request.getNumQuestions(),
                request.getDifficulty()
        );

        if (quizList == null || quizList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }

        // 3️⃣ Content 엔티티 조회
        Content content = contentService.findById(contentId);

        // 4️⃣ 생성된 퀴즈들을 DB에 저장
        quizList.forEach(dto -> {
            try {
                String optionsJson = objectMapper.writeValueAsString(dto.getOptions());
                quizService.saveQuiz(
                        content,
                        dto.getQuestion(),
                        dto.getCorrectAnswer(),
                        optionsJson,
                        dto.getExplanation()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 5️⃣ 생성된 퀴즈 반환
        return ResponseEntity.ok(quizList);
    }

    // ======================================
    // 퀴즈 채점 요청
    // ======================================
    @PostMapping("/{contentId}/quiz/grade")
    public ResponseEntity<Map<String, Object>> gradeQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizGradeRequest request) {

        List<String> pdfPaths = contentService.getPdfPaths(contentId);
        Map<String, Object> result = pythonClient.gradeQuiz(pdfPaths, request.getAnswers());

        if (result == null || result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "채점 실패"));
        }

        return ResponseEntity.ok(result);
    }
}
