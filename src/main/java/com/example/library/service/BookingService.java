package com.example.library.service;

import com.example.library.model.Booking;
import com.example.library.model.Resource;
import com.example.library.model.Resource.ResourceStatus;
import com.example.library.model.User;
import com.example.library.repository.BookingRepository;
import com.example.library.repository.ResourceRepository;
import com.example.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final WaitlistService waitlistService;

    @Autowired
    public BookingService(ResourceRepository resourceRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           WaitlistService waitlistService) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.waitlistService = waitlistService;
    }

    /**
     * Books a resource for a user.
     *
     * THE CONCURRENCY-SAFE PART: we read the Resource using findByIdForUpdate,
     * which takes a DB row lock for the duration of this transaction. If two
     * requests for the SAME resource arrive at the same instant, the database
     * itself serializes them - the second request simply waits until the first
     * transaction commits, then sees the updated (BOOKED) status and is
     * correctly rejected. This is what makes double-booking impossible,
     * regardless of how many users hit the API simultaneously.
     */
    @Transactional
    public Booking bookResource(Long resourceId, Long userId) {
        Resource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Resource is not available right now (status: " + resource.getStatus() +
                    "). Join the waitlist instead.");
        }

        resource.setStatus(ResourceStatus.BOOKED);
        resourceRepository.save(resource);

        Booking booking = new Booking(resource, user, LocalDateTime.now().plusDays(7));
        return bookingRepository.save(booking);
    }

    /**
     * Returns a resource. This is the trigger point for the waitlist system:
     * once a resource becomes free again, we immediately check if anyone is
     * waiting and, if so, notify the next person in line instead of just
     * leaving it open to whoever clicks first.
     */
    @Transactional
    public Booking returnResource(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() == Booking.BookingStatus.RETURNED) {
            throw new IllegalStateException("Booking was already returned");
        }

        booking.setStatus(Booking.BookingStatus.RETURNED);
        booking.setReturnedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        Resource resource = resourceRepository.findByIdForUpdate(booking.getResource().getId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        // Hand off to the waitlist service to decide what happens next -
        // either reserve it for the next person in line, or mark it AVAILABLE.
        waitlistService.onResourceFreed(resource);

        return booking;
    }

    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByBookedAtDesc(userId);
    }
}
