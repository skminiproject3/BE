package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.ProgressStatus;
import com.rookies4.MiniProject3.dto.ProgressDto;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.ProgressRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    // =====================================================
    // 콘텐츠 업로드 시 Progress 자동 생성
    // =====================================================
    public Progress createProgressIfNotExists(Long userId, Long contentId) {
        Optional<Progress> existing = progressRepository.findByUser_IdAndContent_Id(userId, contentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ User not found with id=" + userId));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("❌ Content not found with id=" + contentId));

        Progress progress = Progress.builder()
                .user(user)
                .content(content)
                .recentScore(null) // 처음엔 점수 없음
                .status(ProgressStatus.IN_PROGRESS) // 진행 중 상태
                .build();

        progressRepository.save(progress);
        log.info("🆕 Progress 생성 완료 | userId={} | contentId={} | status={}",
                userId, contentId, progress.getStatus());
        return progress;
    }

    // =====================================================
    // 퀴즈 채점 후 Progress 갱신 (점수 + 상태)
    // =====================================================
    public void updateProgressAfterQuiz(Long userId, Long contentId, Float latestScore) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ User not found with id=" + userId));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("❌ Content not found with id=" + contentId));

        Progress progress = progressRepository.findByUserAndContent(user, content)
                .orElseGet(() -> createProgressIfNotExists(userId, contentId));

        progress.setRecentScore(latestScore);

        // 점수 기준으로 상태 업데이트
        if (latestScore == null) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        } else if (latestScore >= 80.0f) {
            progress.setStatus(ProgressStatus.SUCCESS);
        } else {
            progress.setStatus(ProgressStatus.FAIL);
        }

        progressRepository.save(progress);

        log.info("📊 Progress 갱신 완료 | userId={} | contentId={} | score={} | status={}",
                userId, contentId, latestScore, progress.getStatus());
    }

    // =====================================================
    // 사용자별 전체 학습 현황 조회
    // =====================================================
    @Transactional(readOnly = true)
    public List<ProgressDto.Response> getUserProgress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ User not found with id=" + userId));

        return progressRepository.findByUser(user).stream()
                .map(ProgressDto.Response::fromEntity)
                .toList();
    }

    // =====================================================
    // 콘텐츠별 학습자 진행 현황 조회
    // =====================================================
    @Transactional(readOnly = true)
    public List<ProgressDto.Response> getProgressByContent(Long contentId) {
        List<Progress> progresses = progressRepository.findByContent_Id(contentId);

        return progresses.stream()
                .map(ProgressDto.Response::fromEntity)
                .toList();
    }

    // =====================================================
    // contentId 기준으로 Progress 조회 (단일 사용자용)
    // =====================================================
    @Transactional(readOnly = true)
    public Progress findProgressByContentId(Long contentId) {
        return progressRepository.findByContent_Id(contentId)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
