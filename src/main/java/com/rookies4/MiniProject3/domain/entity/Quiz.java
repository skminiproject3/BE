package com.rookies4.MiniProject3.domain.entity;

import com.rookies4.MiniProject3.domain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "upload_id")
    private Upload upload;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "JSON")
    private String options; // JSON 문자열 저장

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('EASY','MEDIUM','HARD') DEFAULT 'EASY'")
    private Difficulty difficulty = Difficulty.EASY;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Quiz 1 : N QuizAttempt
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> attempts = new ArrayList<>();

}
