package com.rookies4.MiniProject3.domain.entity;

import com.rookies4.MiniProject3.domain.enums.Difficulty;
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
    private Long id;

    // FK → contents.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "JSON", nullable = false)
    private String options;

    @Column(nullable = false, length = 255)
    private String correctAnswer;  // ✅ FastAPI 응답의 answer 매핑

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    public void update(String question, String correctAnswer, String options, Difficulty difficulty) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.options = options;
        this.difficulty = difficulty;
    }
}
