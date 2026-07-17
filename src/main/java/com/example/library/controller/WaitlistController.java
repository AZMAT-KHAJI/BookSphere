package com.example.library.controller;

import com.example.library.dto.Dtos.ApiResponse;
import com.example.library.dto.Dtos.WaitlistJoinRequest;
import com.example.library.model.WaitlistEntry;
import com.example.library.service.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@CrossOrigin
public class WaitlistController {

    private final WaitlistService waitlistService;

    @Autowired
    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> join(@RequestBody WaitlistJoinRequest request) {
        try {
            WaitlistEntry entry = waitlistService.joinWaitlist(request.resourceId, request.userId);
            return ResponseEntity.ok(new ApiResponse(true, "Joined waitlist at position " + entry.getPosition(), entry));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse> confirm(@PathVariable("id") Long waitlistEntryId) {
        try {
            var booking = waitlistService.confirmClaim(waitlistEntryId);
            return ResponseEntity.ok(new ApiResponse(true, "Claimed successfully", booking));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/resource/{resourceId}")
    public List<WaitlistEntry> getWaitlist(@PathVariable Long resourceId) {
        return waitlistService.getWaitlistForResource(resourceId);
    }
}
