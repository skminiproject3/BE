package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.AuthDto;
import com.rookies4.MiniProject3.service.AuthService; // AuthService import
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthDto.SignUpResponse> signup(
            @Valid @RequestBody AuthDto.SignUpRequest request
    ) {
        AuthDto.SignUpResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.TokenResponse token = authService.login(request);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/reissue")
    public ResponseEntity<AuthDto.TokenResponse> reissue(@Valid @RequestBody AuthDto.ReissueRequest request) {
        AuthDto.TokenResponse reissuedToken = authService.reissue(request);
        return ResponseEntity.ok(reissuedToken);
    }
}