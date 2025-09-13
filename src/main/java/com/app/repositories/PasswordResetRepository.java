package com.app.repositories;

import com.app.models.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findTopByEmailOrderByCreatedAtDesc(String email);
    
    // Find active (non-consumed, non-expired) password reset for email
    @Query("SELECT pr FROM PasswordReset pr WHERE pr.email = :email AND pr.consumed = false AND pr.expiresAt > :now ORDER BY pr.createdAt DESC")
    Optional<PasswordReset> findActiveByEmail(String email, LocalDateTime now);
    
    // Find all password resets for an email (for cleanup)
    List<PasswordReset> findByEmailOrderByCreatedAtDesc(String email);
    
    // Delete expired password resets
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordReset pr WHERE pr.expiresAt < :now")
    int deleteExpiredPasswordResets(LocalDateTime now);
    
    // Delete consumed password resets
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordReset pr WHERE pr.consumed = true")
    int deleteConsumedPasswordResets();
}


