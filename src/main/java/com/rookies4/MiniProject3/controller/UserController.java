package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.LoginRequestDto;
import com.rookies4.MiniProject3.dto.LoginResponseDto;
import com.rookies4.MiniProject3.jwt.JwtTokenProvider;
import com.rookies4.MiniProject3.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    // 1️⃣ 로그인 (JWT 발급)
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto loginDto) {

        User user = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

        // JWT 생성
        String token = jwtTokenProvider.createToken(user.getId(), user.getRole().name());

        LoginResponseDto response = LoginResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    // 2️⃣ 내 정보 조회 (토큰 필요)
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {

        User user = userService.findById(userDetails.getId());

        UserDto dto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(dto);
    }
}