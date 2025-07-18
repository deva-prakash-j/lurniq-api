package com.lurniq.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Schema(description = "User entity representing a registered user in the system")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "User's unique identifier", example = "1")
    private Long id;
    
    @Column(unique = true, nullable = false)
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @JsonIgnore
    @Schema(hidden = true)
    private String password;
    
    @Column(name = "first_name")
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    
    @Column(name = "last_name")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Column(name = "profile_picture")
    @Schema(description = "URL to user's profile picture", example = "https://example.com/profile.jpg")
    private String profilePicture;
    
    @Enumerated(EnumType.STRING)
    @Schema(description = "User's role in the system", example = "USER")
    private Role role;
    
    @Enumerated(EnumType.STRING)
    @Schema(description = "User's authentication provider", example = "EMAIL")
    private AuthProvider provider;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
    
    public enum Role {
        USER, ADMIN, CREATOR
    }
    
    public enum AuthProvider {
        LOCAL, GOOGLE
    }
}
