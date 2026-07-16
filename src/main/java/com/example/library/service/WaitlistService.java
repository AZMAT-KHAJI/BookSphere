package com.example.library.service;

import com.example.library.model.*;
import com.example.library.model.Resource.ResourceStatus;
import com.example.library.model.WaitlistEntry.WaitlistStatus;
import com.example.library.repository.BookingRepository;
import com.example.library.repository.ResourceRepository;
import com.example.library.repository.UserRepository;
import com.example.library.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Value("${waitlist.confirmation-window-minutes:2}")
    private int confirmationWindowMinutes;

    @Autowired
    public WaitlistService(WaitlistRepository waitlistRepository,
                            ResourceRepository resourceRepository,
                            UserRepository userRepository,
                            BookingRepository bookingRepository) {
        this.waitlistRepository = waitlistRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * A user joins the queue for a resource that's currently unavailable.
     * Position is simply "how many people are already ahead" + 1 - a plain
     * FIFO queue, which is easy to reason about and easy to explain.
     */
    @Transactional
    public WaitlistEntry joinWaitlist(Long resourceId, Long userId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (resource.getStatus() == ResourceStatus.AVAILABLE) {
            throw new IllegalStateException("Resource is available right now - just book it directly");
        }

        long alreadyInLine = waitlistRepository.countByResourceId(resourceId);
        WaitlistEntry entry = new WaitlistEntry(resource, user, (int) alreadyInLine + 1);
        return waitlistRepository.save(entry);
    }

    /**
     * Called right after a resource is returned. If someone is waiting,
     * we RESERVE the resource for them (so nobody else can grab it) and
     * start their confirmation countdown. If nobody is waiting, the
     * resource simply becomes AVAILABLE for anyone.
     */
    @Transactional
    public void onResourceFreed(Resource resource) {
        var nextInLine = waitlistRepository
                .findFirstByResourceIdAndStatusOrderByPositionAsc(resource.getId(), WaitlistStatus.WAITING);

        if (nextInLine.isEmpty()) {
            resource.setStatus(ResourceStatus.AVAILABLE);
            resourceRepository.save(resource);
            return;
        }

        WaitlistEntry entry = nextInLine.get();
        entry.setStatus(WaitlistStatus.NOTIFIED);
        entry.setNotifiedAt(LocalDateTime.now());
        entry.setExpiresAt(LocalDateTime.now().plusMinutes(confirmationWindowMinutes));
        waitlistRepository.save(entry);

        // RESERVED = held for this specific person, nobody else can book it
        // even though it's technically not checked out yet.
        resource.setStatus(ResourceStatus.RESERVED);
        resourceRepository.save(resource);

        // In a real system: send an email/push notification here.
        // For this project, the frontend dashboard polling is the "notification".
    }

    /**
     * The notified user claims their reserved slot -> becomes a real Booking.
     */
    @Transactional
    public Booking confirmClaim(Long waitlistEntryId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found"));

        if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException("This entry isn't currently eligible to confirm");
        }
        if (entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Your confirmation window has expired");
        }

        entry.setStatus(WaitlistStatus.CONFIRMED);
        waitlistRepository.save(entry);

        Resource resource = resourceRepository.findByIdForUpdate(entry.getResource().getId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        resource.setStatus(ResourceStatus.BOOKED);
        resourceRepository.save(resource);

        Booking booking = new Booking(resource, entry.getUser(), LocalDateTime.now().plusDays(7));
        return bookingRepository.save(booking);
    }

    /**
     * THE SCHEDULED-JOB LOGIC: called every N seconds by WaitlistExpiryScheduler.
     * Finds anyone whose confirmation window passed without confirming, marks
     * them EXPIRED, and automatically promotes the next person in line -
     * recursively, in case that next person is also unresponsive later.
     */
    @Transactional
    public void expireOverdueNotifications() {
        List<WaitlistEntry> overdue = waitlistRepository
                .findByStatusAndExpiresAtBefore(WaitlistStatus.NOTIFIED, LocalDateTime.now());

        for (WaitlistEntry entry : overdue) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);

            Resource resource = resourceRepository.findByIdForUpdate(entry.getResource().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

            // Give the resource back to the queue logic to notify whoever's next
            onResourceFreed(resource);
        }
    }

    public List<WaitlistEntry> getWaitlistForResource(Long resourceId) {
        return waitlistRepository.findByResourceIdOrderByPositionAsc(resourceId);
    }
}
