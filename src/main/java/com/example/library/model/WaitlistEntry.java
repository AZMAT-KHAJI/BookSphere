package com.example.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Position in line - lower number = earlier in queue. Set once at join time.
    private Integer position;

    @Enumerated(EnumType.STRING)
    private WaitlistStatus status;

    private LocalDateTime joinedAt;
    private LocalDateTime notifiedAt;   // when this person was told "it's your turn"
    private LocalDateTime expiresAt;    // deadline to confirm before it passes to next person

    public enum WaitlistStatus {
        WAITING,    // still in line
        NOTIFIED,   // it's their turn, waiting for them to confirm within the window
        CONFIRMED,  // they claimed it -> became a real Booking
        EXPIRED     // they missed their window -> passed to next person
    }

    public WaitlistEntry() {}

    public WaitlistEntry(Resource resource, User user, Integer position) {
        this.resource = resource;
        this.user = user;
        this.position = position;
        this.status = WaitlistStatus.WAITING;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public WaitlistStatus getStatus() { return status; }
    public void setStatus(WaitlistStatus status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
