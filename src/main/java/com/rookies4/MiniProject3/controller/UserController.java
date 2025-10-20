package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @PostMapping("/auth/register")
    public ResponseEntity<User> register(@RequestParam String email,
                                         @RequestParam String password,
                                         @RequestParam String name) {
        User user = userService.register(email, password, name);
        return ResponseEntity.ok(user);
    }

    // 로그인 (간단 예시, JWT 연동 시 토큰 발급)
    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestParam String email,
                                        @RequestParam String password) {
        return userService.findByEmail(email)
                .map(user -> {
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        // JWT 발급 등
                        return ResponseEntity.ok("로그인 성공");
                    } else {
                        return ResponseEntity.status(401).body("비밀번호 불일치");
                    }
                })
                .orElse(ResponseEntity.status(404).body("사용자 없음"));
    }
}