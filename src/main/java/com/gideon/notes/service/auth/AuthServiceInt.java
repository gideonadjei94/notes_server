package com.gideon.notes.service.auth;

import com.gideon.notes.dto.AuthDto;

public interface AuthServiceInt {
    AuthDto.AuthResponse signup(AuthDto.SignupRequest request);
    AuthDto.AuthResponse login(AuthDto.LoginRequest request);
    AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request);
}
