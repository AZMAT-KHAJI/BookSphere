package com.example.library.config;

import com.example.library.model.Resource;
import com.example.library.model.User;
import com.example.library.repository.ResourceRepository;
import com.example.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    public DataSeeder(UserRepository userRepository, ResourceRepository resourceRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void run(String... args) {
        userRepository.save(new User("Asha Rao", "asha@example.com"));
        userRepository.save(new User("Vikram Shah", "vikram@example.com"));
        userRepository.save(new User("Priya Nair", "priya@example.com"));

        resourceRepository.save(new Resource("Meeting Room A", "4-seater, whiteboard, near lobby"));
        resourceRepository.save(new Resource("Oscilloscope #3", "Electronics lab equipment"));
        resourceRepository.save(new Resource("System Design - Alex Xu (book)", "Copy 1 of 2"));
    }
}
