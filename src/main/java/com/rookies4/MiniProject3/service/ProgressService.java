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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    // =====================================================
    // 내부 공용 헬퍼
    // =====================================================
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ User not found with id=" + userId));
    }

    private Content getContentOrThrow(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("❌ Content not found with id=" + contentId));
    }

    /** 점수를 0~100 구간으로 표준화 (null 허용) */
    private Float normalizePercent(Float score) {
        if (score == null) return null;
        float v = score;
        // 서버가 0~1 스케일로 주는 경우 보정
        if (v <= 1.0f) v = v * 100.0f;
        if (v < 0f) v = 0f;
        if (v > 100f) v = 100f;
        // 소수 한 자리 정도만 저장하고 싶으면 주석 해제
        // v = Math.round(v * 10f) / 10f;
        return v;
    }

    /** 점수로 상태를 판정 */
    private ProgressStatus resolveStatusByScore(Float score) {
        if (score == null) return ProgressStatus.IN_PROGRESS;
        if (score >= 80.0f) return ProgressStatus.SUCCESS;
        return ProgressStatus.FAIL;
    }

    // =====================================================
    // (권장) 존재하면 반환, 없으면 생성
    // =====================================================
    @Transactional
    public Progress findOrCreateProgress(Long userId, Long contentId) {
        return progressRepository.findByUser_IdAndContent_Id(userId, contentId)
                .orElseGet(() -> createProgressIfNotExists(userId, contentId));
    }

    // =====================================================
    // 콘텐츠 업로드 시 Progress 자동 생성 (idempotent)
    // =====================================================
    @Transactional
    public Progress createProgressIfNotExists(Long userId, Long contentId) {
        return progressRepository.findByUser_IdAndContent_Id(userId, contentId)
                .orElseGet(() -> {
                    User user = getUserOrThrow(userId);
                    Content content = getContentOrThrow(contentId);

                    Progress progress = Progress.builder()
                            .user(user)
                            .content(content)
                            .recentScore(null) // 처음엔 점수 없음
                            .status(ProgressStatus.IN_PROGRESS)
                            .build();

                    progressRepository.save(progress);
                    log.info("🆕 Progress 생성 완료 | userId={} | contentId={} | status={}",
                            userId, contentId, progress.getStatus());
                    return progress;
                });
    }

    // =====================================================
    // 퀴즈 채점 후 Progress 갱신 (점수 + 상태)
    // =====================================================
    @Transactional
    public void updateProgressAfterQuiz(Long userId, Long contentId, Float latestScoreRaw) {
        User user = getUserOrThrow(userId);
        Content content = getContentOrThrow(contentId);

        Progress progress = progressRepository.findByUserAndContent(user, content)
                .orElseGet(() -> createProgressIfNotExists(userId, contentId));

        Float latestScore = normalizePercent(latestScoreRaw);
        progress.setRecentScore(latestScore);
        progress.setStatus(resolveStatusByScore(latestScore));

        progressRepository.save(progress);

        log.info("📊 Progress 갱신 완료 | userId={} | contentId={} | score={} | status={}",
                userId, contentId, latestScore, progress.getStatus());
    }

    // =====================================================
    // 사용자별 전체 학습 현황 조회
    // =====================================================
    public List<ProgressDto.Response> getUserProgress(Long userId) {
        User user = getUserOrThrow(userId);
        return progressRepository.findByUser(user).stream()
                .map(ProgressDto.Response::fromEntity)
                .toList();
    }

    // =====================================================
    // 콘텐츠별 학습자 진행 현황 조회
    // =====================================================
    public List<ProgressDto.Response> getProgressByContent(Long contentId) {
        List<Progress> progresses = progressRepository.findByContent_Id(contentId);
        return progresses.stream()
                .map(ProgressDto.Response::fromEntity)
                .toList();
    }

    // =====================================================
    // (주의) contentId 기준으로 Progress 조회 — 여러 사용자 중 첫 번째
    //   - 여러 사용자가 같은 contentId를 갖는 환경에서는 부정확할 수 있음.
    //   - 가능하면 findByUserIdAndContentId 사용을 권장.
    // =====================================================
    public Progress findProgressByContentId(Long contentId) {
        return progressRepository.findByContent_Id(contentId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    // =====================================================
    // (추가) 사용자 + 콘텐츠 기준 단건 조회
    // =====================================================
    public Progress findByUserIdAndContentId(Long userId, Long contentId) {
        return progressRepository.findByUser_IdAndContent_Id(userId, contentId).orElse(null);
    }
}
