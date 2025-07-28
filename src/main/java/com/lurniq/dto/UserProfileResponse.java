package com.lurniq.dto;

import com.lurniq.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private String role;
    private boolean emailVerified;
    
    public static UserProfileResponse fromUser(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole().name())
                .emailVerified(user.getEmailVerified())
                .build();
    }
}
