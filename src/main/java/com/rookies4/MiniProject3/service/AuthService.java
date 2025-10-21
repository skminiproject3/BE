package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.RefreshToken;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.dto.AuthDto;
import com.rookies4.MiniProject3.exception.BusinessLogicException;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.jwt.JwtTokenProvider;
import com.rookies4.MiniProject3.repository.RefreshTokenRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthDto.SignUpResponse signup(AuthDto.SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("회원가입 시도 중 이메일 중복: {}", request.getEmail());
            throw new BusinessLogicException(ErrorCode.EMAIL_DUPLICATION);
        }

        User userToSave = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .build();

        User savedUser = userRepository.save(userToSave);

        return AuthDto.SignUpResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .build();

    }

    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        AuthDto.TokenResponse tokenResponse = jwtTokenProvider.generateTokens(authentication);

        // 4. Refresh Token을 DB에 저장 (또는 업데이트)
        RefreshToken refreshToken = RefreshToken.builder()
                .username(authentication.getName())
                .tokenValue(tokenResponse.getRefreshToken())
                .build();

        refreshTokenRepository.findByUsername(authentication.getName())
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refreshToken.getTokenValue()),
                        () -> refreshTokenRepository.save(refreshToken)
                );

        return tokenResponse;
    }

    @Transactional
    public AuthDto.TokenResponse reissue(AuthDto.ReissueRequest request) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Access Token 에서 사용자 정보 (username) 가져오기 (만료된 토큰이어도 가능)
        Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());

        // 3. DB 에서 사용자 username 기반으로 저장된 Refresh Token 값 가져오기
        RefreshToken refreshToken = refreshTokenRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));

        // 4. 요청으로 들어온 Refresh Token 과 DB에 저장된 Refresh Token 값이 일치하는지 검사
        if (!refreshToken.getTokenValue().equals(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 5. 새로운 토큰 생성
        AuthDto.TokenResponse newTokenResponse = jwtTokenProvider.generateTokens(authentication);

        // 6. DB의 Refresh Token 정보 업데이트
        refreshToken.updateToken(newTokenResponse.getRefreshToken());

        // 7. 토큰 발급
        return newTokenResponse;
    }
}
