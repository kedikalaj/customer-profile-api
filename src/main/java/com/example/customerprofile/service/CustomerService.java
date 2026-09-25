package com.example.customerprofile.service;

import com.example.customerprofile.dto.CustomerProfileResponse;
import com.example.customerprofile.dto.PhotoData;
import com.example.customerprofile.dto.UpdateCustomerRequest;
import com.example.customerprofile.exception.CustomerNotFoundException;
import com.example.customerprofile.exception.EmailAlreadyInUseException;
import com.example.customerprofile.exception.InvalidPhotoException;
import com.example.customerprofile.model.Customer;
import com.example.customerprofile.repository.CustomerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CustomerService {

    static final long MAX_PHOTO_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png");

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Cacheable(cacheNames = "customerProfiles", key = "#id")
    public CustomerProfileResponse getProfile(Long id) {
        return CustomerProfileResponse.from(findCustomer(id));
    }

    // Stores the returned (updated) profile in the cache, replacing the old one
    @CachePut(cacheNames = "customerProfiles", key = "#id")
    public CustomerProfileResponse updateProfile(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomer(id);

        if (customerRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new EmailAlreadyInUseException(request.email());
        }

        customer.setName(request.name());
        customer.setEmail(request.email());
        return CustomerProfileResponse.from(customerRepository.save(customer));
    }

    @Cacheable(cacheNames = "customerPhotos", key = "#id")
    public PhotoData getPhoto(Long id) {
        Customer customer = findCustomer(id);
        return new PhotoData(customer.getPhoto(), customer.getPhotoContentType());
    }

    // Removes the old photo from the cache; the next read loads the new one from the database
    @CacheEvict(cacheNames = "customerPhotos", key = "#id")
    public void updatePhoto(Long id, byte[] content, String contentType) {
        validatePhoto(content, contentType);

        Customer customer = findCustomer(id);
        customer.setPhoto(content);
        customer.setPhotoContentType(contentType);
        customerRepository.save(customer);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private void validatePhoto(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new InvalidPhotoException("Photo file must not be empty");
        }
        if (content.length > MAX_PHOTO_SIZE_BYTES) {
            throw new InvalidPhotoException("Photo must be 2 MB or smaller");
        }
        if (contentType == null || !ALLOWED_PHOTO_TYPES.contains(contentType)) {
            throw new InvalidPhotoException("Photo must be a JPEG or PNG image");
        }
    }
}
