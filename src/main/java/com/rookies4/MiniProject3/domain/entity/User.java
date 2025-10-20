package com.rookies4.MiniProject3.domain.entity;

import com.rookies4.MiniProject3.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    private LocalDateTime createdAt = LocalDateTime.now();

    // 관계
    @OneToMany(mappedBy = "user")
    private List<Upload> uploads;

    @OneToMany(mappedBy = "user")
    private List<Progress> progresses;

    @OneToMany(mappedBy = "user")
    private List<ChatLog> chatLogs;

    @OneToMany(mappedBy = "user")
    private List<Log> logs;
}
