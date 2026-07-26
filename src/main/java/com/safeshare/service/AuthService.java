package com.safeshare.service;

import com.safeshare.dto.request.LoginRequest;
import com.safeshare.dto.request.ForgotPasswordRequest;
import com.safeshare.dto.request.RegisterRequest;
import com.safeshare.dto.request.ResetPasswordRequest;
import com.safeshare.dto.response.AuthResponse;
import com.safeshare.dto.response.MessageResponse;
import com.safeshare.entity.AccountToken;
import com.safeshare.entity.AccountTokenType;
import com.safeshare.entity.AuthProvider;
import com.safeshare.entity.User;
import com.safeshare.repository.AccountTokenRepository;
import com.safeshare.repository.UserRepository;
import com.safeshare.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();

        userRepository.save(user);
        AccountToken token = createAccountToken(user, AccountTokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), baseUrl + "/api/auth/verify-email?token=" + token.getToken());

        return MessageResponse.builder()
                .message("Account created. We sent a verification link to your email. Please verify your email before logging in.")
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Please use Google login for this account");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public String verifyEmail(String tokenValue) {
        AccountToken token = getValidToken(tokenValue, AccountTokenType.EMAIL_VERIFICATION);
        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());
        userRepository.save(user);
        accountTokenRepository.save(token);
        return user.getEmail();
    }

    @Transactional
    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getPasswordHash() != null) {
                AccountToken token = createAccountToken(user, AccountTokenType.PASSWORD_RESET);
                emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), baseUrl + "/reset-password.html?token=" + token.getToken());
            }
        });

        return MessageResponse.builder()
                .message("If that email exists, a password reset link has been sent. The link expires in 1 hour.")
                .email(request.getEmail())
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        AccountToken token = getValidToken(request.getToken(), AccountTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());
        userRepository.save(user);
        accountTokenRepository.save(token);

        return MessageResponse.builder()
                .message("Password updated successfully. You can now login to SafeShare.")
                .email(user.getEmail())
                .build();
    }

    private AccountToken createAccountToken(User user, AccountTokenType tokenType) {
        accountTokenRepository.deleteByUserAndTokenType(user, tokenType);
        AccountToken token = AccountToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .tokenType(tokenType)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        return accountTokenRepository.save(token);
    }

    private AccountToken getValidToken(String tokenValue, AccountTokenType tokenType) {
        AccountToken token = accountTokenRepository.findByTokenAndTokenType(tokenValue, tokenType)
                .orElseThrow(() -> new IllegalArgumentException("This link is invalid or has expired"));
        if (token.isUsed() || token.isExpired()) {
            throw new IllegalArgumentException("This link is invalid or has expired");
        }
        return token;
    }
}
