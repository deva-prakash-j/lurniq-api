package com.lurniq.handler;

import com.lurniq.entity.User;
import com.lurniq.repository.UserRepository;
import com.lurniq.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    
    @Value("${oauth2.redirect-url:http://localhost:8080/auth/success}")
    private String redirectUrl;
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, 
            HttpServletResponse response, 
            Authentication authentication
    ) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oAuth2Token.getPrincipal();
        
        String registrationId = oAuth2Token.getAuthorizedClientRegistrationId();
        
        User user = processOAuth2User(oAuth2User, registrationId);
        
        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        // Redirect to configured URL with tokens
        String finalRedirectUrl = String.format(
            "%s?token=%s&refreshToken=%s", 
            redirectUrl, token, refreshToken
        );
        
        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }
    
    private User processOAuth2User(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String email = (String) attributes.get("email");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        String profilePicture = (String) attributes.get("picture");
        String providerId = String.valueOf(attributes.get("sub"));
        
        User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());
        
        return userRepository.findByEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, firstName, lastName, profilePicture))
                .orElseGet(() -> createNewUser(email, firstName, lastName, profilePicture, provider, providerId));
    }
    
    private User updateExistingUser(User existingUser, String firstName, String lastName, String profilePicture) {
        existingUser.setFirstName(firstName);
        existingUser.setLastName(lastName);
        existingUser.setProfilePicture(profilePicture);
        existingUser.setEmailVerified(true);
        return userRepository.save(existingUser);
    }
    
    private User createNewUser(String email, String firstName, String lastName, 
                             String profilePicture, User.AuthProvider provider, String providerId) {
        User newUser = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .profilePicture(profilePicture)
                .provider(provider)
                .providerId(providerId)
                .role(User.Role.USER)
                .emailVerified(true)
                .build();
        
        return userRepository.save(newUser);
    }
}
