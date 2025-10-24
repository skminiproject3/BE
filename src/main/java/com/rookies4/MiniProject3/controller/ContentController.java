package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.repository.UserRepository;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;

    /**
     * 문서 업로드 및 FastAPI 벡터화 요청
     */
    @PostMapping("/upload")
    public ResponseEntity<List<ContentDto.UploadResponse>> uploadContents(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("title") String title,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<ContentDto.UploadResponse> responses = new ArrayList<>();
        String saveDir = "C:/uploads/";
        new File(saveDir).mkdirs();

        // 로그인된 사용자 확인
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자를 찾을 수 없습니다: " + email));
        Long userId = user.getId();

        log.info("📦 파일 업로드 요청 | userId={} | email={} | title={}", userId, email, title);

        for (MultipartFile file : files) {
            try {
                ContentDto.UploadResponse response = contentService.uploadFile(file, title, userId);
                responses.add(response);
            } catch (Exception e) {
                log.error("❌ 파일 업로드 실패 | file={} | message={}", file.getOriginalFilename(), e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    /**
     * FastAPI → 백엔드: 벡터 경로 업데이트
     * (FastAPI가 vector_path를 알려줄 때 호출)
     */
    @PatchMapping("/{contentId}/vector-path")
    public ResponseEntity<String> updateVectorPath(
            @PathVariable Long contentId,
            @RequestParam("vectorPath") String vectorPath
    ) {
        contentService.updateVectorPath(contentId, vectorPath);
        log.info("✅ [VECTOR PATH 저장 완료] contentId={} | vectorPath={}", contentId, vectorPath);
        return ResponseEntity.ok("vectorPath 업데이트 완료: " + vectorPath);
    }

    /**
     * 콘텐츠 처리 상태 조회
     */
    @GetMapping("/{contentId}/status")
    public ResponseEntity<ContentDto.StatusResponse> getContentStatus(@PathVariable Long contentId) {
        ContentDto.StatusResponse response = contentService.getContentStatus(contentId);
        return ResponseEntity.ok(response);
    }
}