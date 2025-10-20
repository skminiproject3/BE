package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.AuthDto;
import com.rookies4.MiniProject3.exception.BusinessLogicException;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.jwt.JwtTokenProvider;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthDto.SignUpResponse signup(AuthDto.SignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("회원가입 시도 중 이메일 중복: {}", request.getEmail());
            throw new BusinessLogicException(ErrorCode.USERNAME_DUPLICATION);
        }

        // 회원 생성
        User userToSave = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .build();

        User savedUser = userRepository.save(userToSave);
        log.info("회원가입 성공: {}", savedUser.getEmail());

        return AuthDto.SignUpResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .build();
    }

    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            return jwtTokenProvider.generateTokens(authentication);
        } catch (BadCredentialsException ex) {
            throw new BusinessLogicException(ErrorCode.INVALID_CREDENTIALS);
        } catch (Exception ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public AuthDto.TokenResponse reissue(AuthDto.ReissueRequest request) {
        try {
            // Refresh Token 유효성 검증
            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                log.warn("재발급 시도 중 유효하지 않은 Refresh Token: {}", request.getRefreshToken());
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            // Access Token에서 사용자 정보 가져오기
            Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());

            // 새로운 토큰 생성
            AuthDto.TokenResponse newTokenResponse = jwtTokenProvider.generateTokens(authentication);
            log.info("토큰 재발급 성공: {}", authentication.getName());
            return newTokenResponse;
        } catch (CustomException ce) {
            throw ce; // 이미 처리된 예외는 그대로 던짐
        } catch (Exception ex) {
            log.error("토큰 재발급 중 예외 발생", ex);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}