package com.example.customerprofile.dto;

import com.example.customerprofile.model.Customer;

import java.io.Serializable;

// Serializable so it can be stored in Redis
public record CustomerProfileResponse(Long id, String name, String email, String photoUrl)
        implements Serializable {

    public static CustomerProfileResponse from(Customer customer) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                "/api/customers/" + customer.getId() + "/photo");
    }
}
