package com.example.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    // Optimistic locking safety net: even though we use pessimistic locking on
    // booking reads, this @Version column protects any other concurrent update
    // path and is a standard concurrency-control pattern worth mentioning in interviews.
    @Version
    private Long version;

    public Resource() {}

    public Resource(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = ResourceStatus.AVAILABLE;
    }

    public enum ResourceStatus {
        AVAILABLE,   // free, anyone can book
        BOOKED,      // currently checked out to someone
        RESERVED     // held for the person at the front of the waitlist (not yet confirmed)
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }
    public Long getVersion() { return version; }
}
