package com.gideon.notes.service.auth;

import com.gideon.notes.dto.AuthDto;
import com.gideon.notes.entity.User;
import com.gideon.notes.enums.UserDomain;
import com.gideon.notes.exception.EntityNotFoundException;
import com.gideon.notes.repository.UserRepository;
import com.gideon.notes.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthServiceInt {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Override
    @Transactional
    public AuthDto.AuthResponse signup(AuthDto.SignupRequest request) {
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }


        if (userRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }


        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .userRole(UserDomain.USER)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepo.save(user);


        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateJwtToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthDto.AuthResponse.builder()
                .token(token)
                .refresh_token(refreshToken)
                .userId(user.getId())
                .username(user.getRealUserName())
                .email(user.getEmail())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );


        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateJwtToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthDto.AuthResponse.builder()
                .token(token)
                .refresh_token(refreshToken)
                .userId(user.getId())
                .username(user.getRealUserName())
                .email(user.getEmail())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        try {
            // Extract email from refresh token
            String email = jwtService.extractUsername(request.getRefreshToken());

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Validate refresh token
            if (!jwtService.isTokenValid(request.getRefreshToken(), userDetails)) {
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            // Get user from database
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Generate new tokens
            String newAccessToken = jwtService.generateJwtToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            return AuthDto.AuthResponse.builder()
                    .token(newAccessToken)
                    .refresh_token(newRefreshToken)
                    .userId(user.getId())
                    .username(user.getRealUserName())
                    .email(user.getEmail())
                    .build();

        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid refresh token: " + e.getMessage());
        }
    }
}