package com.rookies4.MiniProject3.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → progress.id (해당 시도가 어떤 학습 진행에 속하는지) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "progress_id", nullable = false)
    private Progress progress;

    /** 점수 (0~100) */
    @Column(nullable = false)
    private Float score;

    /** 전체 문항 수 */
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    /** 정답 개수 */
    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    // 어떤 회차(batch)의 시험인지 저장
    @Column(name = "quiz_batch", nullable = false)
    private Integer quizBatch;


    /** 시도 시각 */
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
