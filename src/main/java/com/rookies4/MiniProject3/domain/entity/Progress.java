package com.rookies4.MiniProject3.domain.entity;

import com.rookies4.MiniProject3.domain.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → users.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FK → contents.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    /** 퀴즈 시도 기록 */
    @OneToMany(mappedBy = "progress", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> quizAttempts;

    /** 최근 퀴즈 점수 */
    @Column
    private Float recentScore = 0f;

    /** 학습 상태 (IN_PROGRESS, SUCCESS, FAIL) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgressStatus status = ProgressStatus.IN_PROGRESS;

    /** 마지막 접속 시간 */
    @Column(name = "last_accessed_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastAccessedAt = LocalDateTime.now();

    /** 업데이트 시간 자동 반영 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =============================
    // 편의 메서드
    // =============================

    /** 최근 점수 업데이트 + 상태 자동 변경 */
    public void updateRecentScore(Float newScore) {
        this.recentScore = newScore;
        updateStatus();
    }

    /** 상태 자동 계산 */
    private void updateStatus() {
        if (this.recentScore >= 80f) {
            this.status = ProgressStatus.SUCCESS;
        } else if (this.recentScore > 0f) {
            this.status = ProgressStatus.FAIL;
        } else {
            this.status = ProgressStatus.IN_PROGRESS;
        }
    }

    /** 최근 접속 갱신 */
    public void touchAccess() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
