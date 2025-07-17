package com.lurniq.repository;

import com.lurniq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
    
    Boolean existsByEmail(String email);
}
