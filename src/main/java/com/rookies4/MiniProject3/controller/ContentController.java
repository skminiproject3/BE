package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.repository.UserRepository;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.ProgressService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final PythonServerClient pythonClient; // (사용 안 해도 무방)
    private final UserRepository userRepository;
    private final ProgressService progressService;

    /** 📂 문서 업로드 + Progress 생성 */
    @PostMapping("/upload")
    public ResponseEntity<List<ContentDto.UploadResponse>> uploadContents(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("title") String title,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }

        List<ContentDto.UploadResponse> responses = new ArrayList<>();
        String saveDir = "C:/uploads/"; // FileStorageService가 다른 경로를 쓰면 이건 생략 가능
        new File(saveDir).mkdirs();

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자를 찾을 수 없습니다: " + email));
        Long userId = user.getId();

        log.info("📦 파일 업로드 요청 | userId={} | email={} | title={}", userId, email, title);

        for (MultipartFile file : files) {
            try {
                // 1) 콘텐츠 업로드 및 DB 저장
                ContentDto.UploadResponse response = contentService.uploadFile(file, title, userId);
                responses.add(response);

                // 2) Progress 자동 생성
                progressService.createProgressIfNotExists(userId, response.getContentId());
                log.info("🧩 Progress 생성 완료 | userId={} | contentId={}", userId, response.getContentId());
            } catch (Exception e) {
                log.error("❌ 파일 업로드 실패 | file={} | message={}", file.getOriginalFilename(), e.getMessage());
            }
        }
        return ResponseEntity.ok(responses);
    }

    /** ✅ 내 콘텐츠 목록 조회: GET /api/contents */
    @GetMapping
    public ResponseEntity<?> listMyContents(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        String email = userDetails.getUsername();
        Optional<User> meOpt = userRepository.findByEmail(email);
        if (meOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
        Long userId = meOpt.get().getId();

        // 서비스에서 사용자별 콘텐츠 조회
        List<Content> contents = contentService.findByUserId(userId);

        // ⚠️ 필드명 수정: uploadStatus → status
        List<Map<String, Object>> dto = contents.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("contentId", c.getId());
            m.put("title", c.getTitle());
            m.put("status", String.valueOf(c.getStatus())); // ← 여기!
            m.put("createdAt", c.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    /** ✅ 단건 조회: GET /api/contents/{contentId} */
    @GetMapping("/{contentId}")
    public ResponseEntity<?> getContent(
            @PathVariable Long contentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            Content content = contentService.findById(contentId);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("contentId", content.getId());
            m.put("title", content.getTitle());
            m.put("status", String.valueOf(content.getStatus())); // ← 여기!
            m.put("createdAt", content.getCreatedAt());
            m.put("vectorPath", content.getVectorPath());
            return ResponseEntity.ok(m);
        } catch (RuntimeException ex) {
            // contentService.findById 가 못 찾으면 예외를 던지는 구현 → 404로 매핑
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "콘텐츠를 찾을 수 없습니다."));
        }
    }

    /** FastAPI → 백엔드: 벡터 경로 업데이트 */
    @PatchMapping("/{contentId}/vector-path")
    public ResponseEntity<String> updateVectorPath(
            @PathVariable Long contentId,
            @RequestParam("vectorPath") String vectorPath
    ) {
        contentService.updateVectorPath(contentId, vectorPath);
        log.info("✅ [VECTOR PATH 저장 완료] contentId={} | vectorPath={}", contentId, vectorPath);
        return ResponseEntity.ok("vectorPath 업데이트 완료: " + vectorPath);
    }

    /** 콘텐츠 처리 상태 조회 */
    @GetMapping("/{contentId}/status")
    public ResponseEntity<ContentDto.StatusResponse> getContentStatus(@PathVariable Long contentId) {
        ContentDto.StatusResponse response = contentService.getContentStatus(contentId);
        return ResponseEntity.ok(response);
    }
}
