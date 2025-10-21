
package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.dto.SummaryDto;
import com.rookies4.MiniProject3.dto.SummaryDto.Response;
import com.rookies4.MiniProject3.exception.EntityNotFoundException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.UserRepository;
import com.rookies4.MiniProject3.service.ContentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final UserRepository userRepository;

    // 문서 업로드 및 처리 요청
    @PostMapping
    public ResponseEntity<ContentDto.UploadResponse> uploadContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. UserDetails 에서 이메일(username) 추출
        String email = userDetails.getUsername();

        // 2. 이메일을 사용해 User 엔티티 조회 -> 사용자 Id 확보
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 3. 추출한 사용자 Id 사용
        Long currentUserId = user.getId();

        ContentDto.UploadResponse response = contentService.uploadAndProcessFile(file, title, currentUserId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // 콘텐츠 처리 상태 조회
    @GetMapping("/{contentId}/status")
    public ResponseEntity<ContentDto.StatusResponse> getContentStatus(@PathVariable Long contentId) {
        ContentDto.StatusResponse response = contentService.getContentStatus(contentId);

        return ResponseEntity.ok(response);
    }

    // 챕터별 요약 목록 조회
    @GetMapping("/{contentId}/summaries")
    public ResponseEntity<List<Response>> getSummaries(@PathVariable Long contentId) {
        List<SummaryDto.Response> response = contentService.getSummaries(contentId);

        return ResponseEntity.ok(response);
    }
}
