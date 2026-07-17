package com.example.library.controller;

import com.example.library.dto.Dtos.ApiResponse;
import com.example.library.dto.Dtos.BookingRequest;
import com.example.library.model.Booking;
import com.example.library.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> book(@RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.bookResource(request.resourceId, request.userId);
            return ResponseEntity.ok(new ApiResponse(true, "Booked successfully", booking));
        } catch (IllegalStateException e) {
            // Resource not available - this is the expected "someone else beat you to it" case
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse(false, e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse> returnBooking(@PathVariable("id") Long bookingId) {
        try {
            Booking booking = bookingService.returnResource(bookingId);
            return ResponseEntity.ok(new ApiResponse(true, "Returned successfully", booking));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getBookingsForUser(@PathVariable Long userId) {
        return bookingService.getBookingsForUser(userId);
    }
}
