package com.example.customerprofile.repository;

import com.example.customerprofile.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Spring Data builds the query from the method name:
    // "is there a customer with this email whose id is NOT this id?"
    boolean existsByEmailAndIdNot(String email, Long id);
}
