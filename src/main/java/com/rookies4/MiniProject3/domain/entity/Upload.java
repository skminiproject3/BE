package com.rookies4.MiniProject3.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    private Long fileSize;

    private String fileType;

    private LocalDateTime uploadDate = LocalDateTime.now();

    // 관계
    @OneToMany(mappedBy = "upload")
    private List<Summary> summaries;

    @OneToMany(mappedBy = "upload")
    private List<Quiz> quizzes;

    @OneToMany(mappedBy = "upload")
    private List<Progress> progresses;

    @OneToMany(mappedBy = "upload")
    private List<ChatLog> chatLogs;
}