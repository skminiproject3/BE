package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.ProgressRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;

    // ======================================
    // 콘텐츠 업로드 및 DB 저장
    // ======================================
    @Transactional
    public ContentDto.UploadResponse saveToDb(String filePath, String fileName, String title, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("❌ User not found with id: " + userId));

            Content content = Content.builder()
                    .user(user)
                    .filePath(filePath)
                    .fileName(fileName)
                    .title(title)
                    .status(ContentStatus.PROCESSING)
                    .build();

            Content saved = contentRepository.save(content);

            log.info("✅ 콘텐츠 저장 완료: id={}, title={}, user={}", saved.getId(), saved.getTitle(), user.getUsername());

            return new ContentDto.UploadResponse(
                    saved.getId(),
                    saved.getTitle(),
                    saved.getStatus().name(),
                    null
            );

        } catch (Exception e) {
            log.error("🚨 DB 저장 실패", e);
            throw new RuntimeException("콘텐츠 저장 중 오류 발생");
        }
    }

    // ======================================
    // 콘텐츠 상태 조회
    // ======================================
    public ContentDto.StatusResponse getContentStatus(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("❌ 콘텐츠를 찾을 수 없습니다. id=" + contentId));

        return new ContentDto.StatusResponse(content.getStatus().name());
    }

    // ======================================
    // PDF 경로 조회 (Python 서버 연동용)
    // ======================================
    public List<String> getPdfPaths(Long contentId) {
        Optional<Content> contentOpt = contentRepository.findById(contentId);
        if (contentOpt.isEmpty()) {
            log.error("❌ getPdfPaths: 콘텐츠 ID {} 를 찾을 수 없습니다.", contentId);
            return Collections.emptyList();
        }

        Content content = contentOpt.get();
        return List.of(content.getFilePath());
    }

    // ======================================
    // 단일 콘텐츠 조회
    // ======================================
    public Content findById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Content not found with id: " + id));
    }

    // ======================================
    // 콘텐츠 전체 목록 조회
    // ======================================
    public List<Content> getAllContents() {
        return contentRepository.findAll();
    }

    // ======================================
    // 상태 업데이트 (예: 요약 완료 등)
    // ======================================
    @Transactional
    public void updateStatus(Long contentId, ContentStatus status) {
        Optional<Content> optional = contentRepository.findById(contentId);
        if (optional.isPresent()) {
            Content content = optional.get();
            content.setStatus(status);
            contentRepository.save(content);
        } else {
            log.warn("⚠️ updateStatus: 콘텐츠 ID {} 없음", contentId);
        }
    }

    // ======================================
    // ✅ Progress 조회 + 없으면 자동 생성
    // ======================================
    @Transactional
    public Progress findProgressByContentId(Long contentId) {
        try {
            Optional<Progress> existing = progressRepository.findByContent_Id(contentId);
            if (existing.isPresent()) {
                log.info("✅ 기존 Progress 조회 완료 (id={})", existing.get().getId());
                return existing.get();
            }

            // 없을 경우 새로 생성
            log.warn("⚠️ Progress 없음 → 새로 생성 시도 (contentId={})", contentId);
            return createProgressForUserAndContent(contentId);

        } catch (Exception e) {
            log.error("🚨 Progress 조회 실패 (contentId={}): {}", contentId, e.getMessage());
            return null;
        }
    }

    // ======================================
    // ✅ Progress 자동 생성 (quiz_attempts 연동용)
    // ======================================
    @Transactional
    public Progress createProgressForUserAndContent(Long contentId) {
        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("❌ 콘텐츠를 찾을 수 없습니다. id=" + contentId));

            User user = content.getUser();
            if (user == null) {
                log.error("❌ Progress 생성 실패: 콘텐츠에 연결된 User가 없습니다. (contentId={})", contentId);
                return null;
            }

            Progress progress = Progress.builder()
                    .user(user)
                    .content(content)
                    .completedChapters(0)
                    .averageScore(0f)
                    .build();

            Progress saved = progressRepository.save(progress);
            log.info("🆕 Progress 새로 생성 완료: id={}, user={}, contentId={}",
                    saved.getId(), user.getUsername(), contentId);

            return saved;

        } catch (Exception e) {
            log.error("🚨 Progress 생성 실패 (contentId={}): {}", contentId, e.getMessage());
            return null;
        }
    }
}
