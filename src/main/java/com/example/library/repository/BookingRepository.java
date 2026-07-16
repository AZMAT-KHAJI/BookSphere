package com.example.library.repository;

import com.example.library.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);
    List<Booking> findByResourceIdAndStatus(Long resourceId, Booking.BookingStatus status);
}
