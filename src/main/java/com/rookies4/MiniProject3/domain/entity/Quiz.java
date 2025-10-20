package com.rookies4.MiniProject3.domain.entity;

import com.rookies4.MiniProject3.domain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
}
