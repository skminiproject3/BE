package com.rookies4.MiniProject3.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 고유 PK

    // 콘텐츠별 퀴즈 순서 ID (1, 2, 3...)
    @Column(name = "quiz_id")
    private Integer quizId;

    // 한 번 생성한 "퀴즈 세트(회차)" 번호
    // 예) 첫 생성한 묶음 = 1, 두 번째 다시 생성한 묶음 = 2
    @Column(name = "quiz_batch", nullable = false)
    private Integer quizBatch;

    // FK → contents.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "JSON", nullable = false)
    private String options;

    @Column(nullable = false, length = 255)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;
}