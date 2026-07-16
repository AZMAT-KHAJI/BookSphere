package com.example.library.repository;

import com.example.library.model.WaitlistEntry;
import com.example.library.model.WaitlistEntry.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByResourceIdOrderByPositionAsc(Long resourceId);

    // Next person in line who hasn't been notified yet - lowest position wins
    Optional<WaitlistEntry> findFirstByResourceIdAndStatusOrderByPositionAsc(
            Long resourceId, WaitlistStatus status);

    // Used to compute the next position number when someone joins the queue
    long countByResourceId(Long resourceId);

    // Used by the scheduled job to find anyone whose confirmation window has passed
    List<WaitlistEntry> findByStatusAndExpiresAtBefore(WaitlistStatus status, LocalDateTime now);
}
