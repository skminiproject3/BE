package com.rookies4.MiniProject3.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // 사용자 식별을 위해 username 사용

    @Column(nullable = false)
    private String tokenValue;

    @Builder
    public RefreshToken(String username, String tokenValue) {
        this.username = username;
        this.tokenValue = tokenValue;
    }

    public void updateToken(String newTokenValue) {
        this.tokenValue = newTokenValue;
    }
}
