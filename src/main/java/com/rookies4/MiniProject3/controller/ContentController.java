package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.dto.SummaryChapterRequest;
import com.rookies4.MiniProject3.dto.SummaryDto.Response;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final PythonServerClient pythonClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ======================================
    // 📂 문서 업로드 및 DB 저장
    // ======================================
    @PostMapping("/upload")
    public ResponseEntity<List<ContentDto.UploadResponse>> uploadContents(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("title") String title) {

        List<File> savedFiles = new ArrayList<>();
        List<ContentDto.UploadResponse> responses = new ArrayList<>();
        String saveDir = "C:/uploads/";
        new File(saveDir).mkdirs();

        try {
            for (MultipartFile file : files) {
                String uuidFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                File savedFile = new File(saveDir + uuidFileName);
                file.transferTo(savedFile);
                savedFiles.add(savedFile);

                ContentDto.UploadResponse response = contentService.saveToDb(
                        savedFile.getAbsolutePath(),
                        file.getOriginalFilename(),
                        title,
                        1L // ⚠️ 임시 userId=1
                );
                responses.add(response);
            }

            // ✅ Python 서버 업로드 요청
            List<String> pythonUploadResult = pythonClient.uploadPDFs(savedFiles);
            log.info("📤 Python 서버 업로드 완료: {}", pythonUploadResult);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(responses);

        } catch (Exception e) {
            log.error("🚨 파일 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    // ======================================
    // 📋 콘텐츠 상태 조회
    // ======================================
    @GetMapping("/{contentId}/status")
    public ResponseEntity<ContentDto.StatusResponse> getContentStatus(@PathVariable Long contentId) {
        try {
            ContentDto.StatusResponse response = contentService.getContentStatus(contentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("🚨 콘텐츠 상태 조회 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ContentDto.StatusResponse("NOT_FOUND"));
        }
    }

    // ======================================
    // 📘 전체 요약 요청
    // ======================================
    @PostMapping("/{contentId}/summarize")
    public ResponseEntity<String> summarizeContent(@PathVariable Long contentId) {
        try {
            List<String> pdfPaths = contentService.getPdfPaths(contentId);
            if (pdfPaths.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("❌ PDF 경로를 찾을 수 없습니다.");
            }

            String summary = pythonClient.summarize(pdfPaths);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("🚨 요약 요청 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("요약 중 오류 발생");
        }
    }

    // ======================================
    // 📚 단원별 요약 요청
    // ======================================
    @PostMapping("/{contentId}/summaries")
    public ResponseEntity<List<Response>> getSummaries(
            @PathVariable Long contentId,
            @RequestBody SummaryChapterRequest request) {

        try {
            List<String> pdfPaths = contentService.getPdfPaths(contentId);
            if (pdfPaths.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.emptyList());
            }

            List<Response> summaries = pythonClient.summarizeChapters(pdfPaths, request.getChapterRequest());
            return ResponseEntity.ok(summaries);

        } catch (Exception e) {
            log.error("🚨 단원별 요약 요청 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    // ======================================
    // 💬 질문 응답 요청
    // ======================================
    @PostMapping("/{contentId}/ask")
    public ResponseEntity<String> askQuestion(
            @PathVariable Long contentId,
            @RequestParam("question") String question) {

        try {
            List<String> pdfPaths = contentService.getPdfPaths(contentId);
            if (pdfPaths.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("❌ PDF 경로를 찾을 수 없습니다.");
            }

            String answer = pythonClient.answerQuestion(question, pdfPaths);
            return ResponseEntity.ok(answer);

        } catch (Exception e) {
            log.error("🚨 질문 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("질문 처리 중 오류 발생");
        }
    }
}
