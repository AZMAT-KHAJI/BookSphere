package com.example.library.repository;

import com.example.library.model.Resource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    /**
     * PESSIMISTIC_WRITE places a DB-level row lock on this resource until the
     * enclosing transaction commits/rolls back. If two users hit "book" on the
     * same resource at the same millisecond, the second transaction blocks here
     * until the first one finishes - so it always sees the up-to-date status
     * and can never book an already-taken resource. This is what prevents the
     * classic "double booking" race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(Long id);
}
