package com.rookies4.MiniProject3.service;


import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.jwt.JwtTokenProvider;
import com.rookies4.MiniProject3.repository.UserRepository;
import com.rookies4.MiniProject3.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    public User register(String email, String password, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("[ERROR] 이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .username(username)
                .build();

        return userRepository.save(user);
    }

    // 로그인 처리
    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("[ERROR] 이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        Map<String, Object> result = new HashMap<>();
        result.put("grantType", "Bearer");
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", jwtTokenProvider.getExpiration(accessToken));

        return result;
    }
}