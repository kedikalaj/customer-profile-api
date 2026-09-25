package com.example.customerprofile.config;

import com.example.customerprofile.model.Customer;
import com.example.customerprofile.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

// Inserts two customers at startup, because the API only reads and updates existing customers
@Component
public class DataSeeder implements CommandLineRunner {

    // A tiny 1x1 pixel PNG, so every seeded customer has the mandatory photo
    private static final byte[] PLACEHOLDER_PHOTO = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    private final CustomerRepository customerRepository;

    public DataSeeder(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }
        customerRepository.saveAll(List.of(
                new Customer("Ada Lovelace", "ada@example.com", PLACEHOLDER_PHOTO, "image/png"),
                new Customer("Alan Turing", "alan@example.com", PLACEHOLDER_PHOTO, "image/png")));
    }
}
