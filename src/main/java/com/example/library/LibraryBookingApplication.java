package com.example.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // needed so our waitlist-expiry background job actually runs
public class LibraryBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryBookingApplication.class, args);
    }
}
