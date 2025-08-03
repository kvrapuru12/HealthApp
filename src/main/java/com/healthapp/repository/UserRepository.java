package com.healthapp.repository;

import com.healthapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    boolean existsByEmailAndIdNot(String email, Long id);
    
    // Filtering methods
    Page<User> findByAccountStatus(User.AccountStatus status, Pageable pageable);
    
    Page<User> findByRole(User.UserRole role, Pageable pageable);
    
    Page<User> findByAccountStatusAndRole(User.AccountStatus status, User.UserRole role, Pageable pageable);
    
    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        String firstName, String lastName, Pageable pageable);
} 