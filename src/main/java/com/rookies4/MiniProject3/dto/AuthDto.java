package com.rookies4.MiniProject3.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.rookies4.MiniProject3.domain.entity.User;
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
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        private String password;

        @NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
        private String username;
    }

    @Getter
    public static class LoginRequest {
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        private String password;
    }

    @Getter
    @Builder
    public static class SignUpResponse {
        private Long userId;
        private String email;
        private String username;

        public static SignUpResponse fromEntity(User user) {
            return SignUpResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class TokenResponse {
        private String grantType = "Bearer";
        private String accessToken;
        private String refreshToken;
        private long expiresIn;
    }

    @Getter
    @NoArgsConstructor
    public static class ReissueRequest {
        private String accessToken;
        private String refreshToken;
    }
}