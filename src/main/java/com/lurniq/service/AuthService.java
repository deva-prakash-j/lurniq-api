package com.lurniq.service;

import com.lurniq.dto.*;
import com.lurniq.entity.User;
import com.lurniq.repository.UserRepository;
import com.lurniq.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;
    
    public RegistrationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .provider(User.AuthProvider.LOCAL)
                .emailVerified(false)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Send activation email (async)
        try {
            emailVerificationService.sendActivationEmail(savedUser);
        } catch (Exception e) {
            // Log the error but don't fail the registration
            System.err.println("Failed to send activation email: " + e.getMessage());
        }
        
        // Return secure registration response (no tokens until email verified)
        return RegistrationResponse.builder()
                .success(true)
                .message("Account created successfully! Please check your email to activate your account.")
                .user(RegistrationResponse.UserInfo.builder()
                        .id(savedUser.getId())
                        .email(savedUser.getEmail())
                        .firstName(savedUser.getFirstName())
                        .lastName(savedUser.getLastName())
                        .emailVerified(savedUser.getEmailVerified())
                        .build())
                .nextStep("email_verification")
                .build();
    }
    
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Create authentication token with web details to preserve IP address
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        );
        
        // Set web authentication details to capture IP address
        authToken.setDetails(new WebAuthenticationDetails(httpRequest));
        
        authenticationManager.authenticate(authToken);
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if email is verified before issuing tokens
        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email not verified. Please check your email and activate your account.");
        }
        
        String jwtToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(UserProfileResponse.fromUser(user))
                .build();
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        String userEmail = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(UserProfileResponse.fromUser(user))
                .build();
    }
}
