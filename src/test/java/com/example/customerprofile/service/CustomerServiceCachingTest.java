package com.example.customerprofile.service;

import com.example.customerprofile.TestcontainersConfiguration;
import com.example.customerprofile.dto.CustomerProfileResponse;
import com.example.customerprofile.dto.PhotoData;
import com.example.customerprofile.dto.UpdateCustomerRequest;
import com.example.customerprofile.model.Customer;
import com.example.customerprofile.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Runs against a real Redis (started in Docker by TestcontainersConfiguration).
// The repository is mocked so we can count how often the database would be hit.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CustomerServiceCachingTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CustomerRepository customerRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).invalidate());

        customer = new Customer("Ada Lovelace", "ada@example.com", new byte[]{1, 2, 3}, "image/png");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
    }

    @Test
    void getProfile_secondCallIsServedFromRedis() {
        customerService.getProfile(1L);
        customerService.getProfile(1L);

        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void updateProfile_replacesCachedProfile() {
        customerService.getProfile(1L); // Redis now holds "Ada Lovelace"
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.updateProfile(1L, new UpdateCustomerRequest("Ada King", "ada.king@example.com"));
        CustomerProfileResponse profile = customerService.getProfile(1L);

        assertThat(profile.name()).isEqualTo("Ada King");
        // 1st getProfile + inside updateProfile; the last read was served from Redis
        verify(customerRepository, times(2)).findById(1L);
    }

    @Test
    void updatePhoto_evictsCachedPhoto() {
        customerService.getPhoto(1L); // Redis now holds the PNG

        customerService.updatePhoto(1L, new byte[]{9}, "image/jpeg");
        PhotoData photo = customerService.getPhoto(1L);

        assertThat(photo.contentType()).isEqualTo("image/jpeg");
        // 1st getPhoto + inside updatePhoto + reload after eviction
        verify(customerRepository, times(3)).findById(1L);
    }
}
