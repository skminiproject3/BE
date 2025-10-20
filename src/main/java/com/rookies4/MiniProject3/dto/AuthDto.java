package com.rookies4.MiniProject3.dto;

import com.rookies4.MiniProject3.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Builder;
import lombok.*;
import java.time.LocalDate;


public class AuthDto {
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SignUpRequest {
        @NotBlank(message = "아이디는 필수 입력 항목입니다.")
        private String username;
        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        private String password;
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        private String nickname;
        @NotNull(message = "생년월일은 필수 입력 항목입니다.")
        private LocalDate birthdate;
        private String profileImageUrl;
    }

    @Getter
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Getter
    public static class SignUpResponse {
        private Long userId;
        private String username;
        private String nickname;
        private Role role;

        @Builder
        public SignUpResponse(Long userId, String username, String nickname, Role role) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.role = role;
        }
    }

    @Getter
    public static class TokenResponse {
        private String grantType = "Bearer";
        private String accessToken;
        private String refreshToken;
        private long expiresIn;

        @Builder
        public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    // Token 재발급 요청을 위한 DTO
    @Getter
    @NoArgsConstructor
    public static class ReissueRequest {
        private String accessToken;
        private String refreshToken;
    }
}