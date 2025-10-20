package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.repository.UserRepository;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public User register(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    // 이메일로 조회
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}