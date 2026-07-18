package com.example.library.dto;

public class Dtos {

    public static class CreateUserRequest {
        public String name;
        public String email;
    }

    public static class CreateResourceRequest {
        public String name;
        public String description;
    }

    public static class BookingRequest {
        public Long resourceId;
        public Long userId;
    }

    public static class WaitlistJoinRequest {
        public Long resourceId;
        public Long userId;
    }

    // Generic simple response wrapper so the frontend gets a clear success/message pair
    public static class ApiResponse {
        public boolean success;
        public String message;
        public Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
}
