package com.example.library.scheduler;

import com.example.library.service.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WaitlistExpiryScheduler {

    private final WaitlistService waitlistService;

    @Autowired
    public WaitlistExpiryScheduler(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    /**
     * Runs every 15 seconds and checks: has anyone's confirmation window
     * expired? If so, auto-expire them and promote the next person.
     *
     * This is the background "worker" piece of the system - the same pattern
     * used for things like cart-reservation timeouts on e-commerce checkouts,
     * or ticket-hold timers on booking sites. In interviews this is your
     * answer to "how did you handle time-based state transitions?"
     */
    @Scheduled(fixedRate = 15000)
    public void checkExpiredNotifications() {
        waitlistService.expireOverdueNotifications();
    }
}
