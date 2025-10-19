package com.app.repositories;

import com.app.models.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findByEmail(String email);
    void deleteByEmail(String email);
    
    // Find active (non-consumed, non-expired) registrations
    @Query("SELECT p FROM PendingRegistration p WHERE p.email = :email AND p.consumed = false AND p.expiresAt > :now")
    Optional<PendingRegistration> findActiveByEmail(String email, LocalDateTime now);
    
    // Find all expired registrations
    @Query("SELECT p FROM PendingRegistration p WHERE p.expiresAt < :now")
    List<PendingRegistration> findExpiredRegistrations(LocalDateTime now);
    
    // Find all consumed registrations
    @Query("SELECT p FROM PendingRegistration p WHERE p.consumed = true")
    List<PendingRegistration> findConsumedRegistrations();
    
    // Delete expired registrations
    @Modifying
    @Transactional
    @Query("DELETE FROM PendingRegistration p WHERE p.expiresAt < :now")
    void deleteExpiredRegistrations(LocalDateTime now);
    
    // Delete consumed registrations
    @Modifying
    @Transactional
    @Query("DELETE FROM PendingRegistration p WHERE p.consumed = true")
    void deleteConsumedRegistrations();
}


