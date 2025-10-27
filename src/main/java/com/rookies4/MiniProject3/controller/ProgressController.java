package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.ProgressDto;
import com.rookies4.MiniProject3.repository.UserRepository;
import com.rookies4.MiniProject3.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;
    private final UserRepository userRepository;

    // ✅ 내 진행 현황: /api/progress/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyProgress(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        String email = principal.getUsername();
        Optional<User> me = userRepository.findByEmail(email);
        if (me.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
        List<ProgressDto.Response> response = progressService.getUserProgress(me.get().getId());
        return ResponseEntity.ok(response);
    }

    // ✅ 이메일로 진행 현황: /api/progress/email/{email}
    //   (프론트가 /api/progress/sss@naver.com 을 호출했다면 이쪽으로 변경 권장)
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getProgressByEmail(@PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
        List<ProgressDto.Response> response = progressService.getUserProgress(user.get().getId());
        return ResponseEntity.ok(response);
    }

    // ✅ userId로 진행 현황: /api/progress/user/{userId}
    //   (기존 "/{userId}" 는 "me" 같은 문자열과 충돌하므로 경로를 바꿉니다)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProgressDto.Response>> getUserProgress(@PathVariable Long userId) {
        log.info("📘 사용자 학습 현황 조회 | userId={}", userId);
        List<ProgressDto.Response> response = progressService.getUserProgress(userId);
        return ResponseEntity.ok(response);
    }

    // ✅ 콘텐츠별 진행 현황: /api/progress/content/{contentId}
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<ProgressDto.Response>> getProgressByContent(@PathVariable Long contentId) {
        log.info("📗 콘텐츠 학습 현황 조회 | contentId={}", contentId);
        List<ProgressDto.Response> response = progressService.getProgressByContent(contentId);
        return ResponseEntity.ok(response);
    }
}
