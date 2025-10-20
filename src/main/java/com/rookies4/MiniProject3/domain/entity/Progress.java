package com.rookies4.MiniProject3.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "upload_id")
    private Upload upload;

    private Integer chapterCompleted = 0;
    private Integer totalChapters = 0;
    private Float quizScore = 0f;
    private LocalDateTime lastAccessed = LocalDateTime.now();
}
