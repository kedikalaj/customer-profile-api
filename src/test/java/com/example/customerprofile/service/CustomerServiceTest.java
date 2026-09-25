package com.example.customerprofile.service;

import com.example.customerprofile.dto.CustomerProfileResponse;
import com.example.customerprofile.dto.PhotoData;
import com.example.customerprofile.dto.UpdateCustomerRequest;
import com.example.customerprofile.exception.CustomerNotFoundException;
import com.example.customerprofile.exception.EmailAlreadyInUseException;
import com.example.customerprofile.exception.InvalidPhotoException;
import com.example.customerprofile.model.Customer;
import com.example.customerprofile.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer("Ada Lovelace", "ada@example.com", new byte[]{1, 2, 3}, "image/png");
        // The database normally assigns the id; in a unit test we set it ourselves
        ReflectionTestUtils.setField(customer, "id", 1L);
    }

    @Test
    void getProfile_returnsProfile_whenCustomerExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerProfileResponse profile = customerService.getProfile(1L);

        assertThat(profile.id()).isEqualTo(1L);
        assertThat(profile.name()).isEqualTo("Ada Lovelace");
        assertThat(profile.email()).isEqualTo("ada@example.com");
        assertThat(profile.photoUrl()).isEqualTo("/api/customers/1/photo");
    }

    @Test
    void getProfile_throwsNotFound_whenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getProfile(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer 99 not found");
    }

    @Test
    void updateProfile_savesNewNameAndEmail() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("ada.king@example.com", 1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileResponse updated = customerService.updateProfile(1L,
                new UpdateCustomerRequest("Ada King", "ada.king@example.com"));

        assertThat(updated.name()).isEqualTo("Ada King");
        assertThat(updated.email()).isEqualTo("ada.king@example.com");
        verify(customerRepository).save(customer);
    }

    @Test
    void updateProfile_throwsConflict_whenEmailBelongsToAnotherCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("alan@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateProfile(1L,
                new UpdateCustomerRequest("Ada", "alan@example.com")))
                .isInstanceOf(EmailAlreadyInUseException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateProfile_throwsNotFound_whenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateProfile(99L,
                new UpdateCustomerRequest("Ada", "ada@example.com")))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void getPhoto_returnsContentAndType() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        PhotoData photo = customerService.getPhoto(1L);

        assertThat(photo.content()).containsExactly(1, 2, 3);
        assertThat(photo.contentType()).isEqualTo("image/png");
    }

    @Test
    void updatePhoto_savesNewPhoto() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        byte[] newPhoto = {9, 8, 7};

        customerService.updatePhoto(1L, newPhoto, "image/jpeg");

        assertThat(customer.getPhoto()).isEqualTo(newPhoto);
        assertThat(customer.getPhotoContentType()).isEqualTo("image/jpeg");
        verify(customerRepository).save(customer);
    }

    @Test
    void updatePhoto_rejectsEmptyFile() {
        assertThatThrownBy(() -> customerService.updatePhoto(1L, new byte[0], "image/png"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessage("Photo file must not be empty");

        verifyNoInteractions(customerRepository);
    }

    @Test
    void updatePhoto_rejectsFileLargerThan2Mb() {
        byte[] tooBig = new byte[(int) CustomerService.MAX_PHOTO_SIZE_BYTES + 1];

        assertThatThrownBy(() -> customerService.updatePhoto(1L, tooBig, "image/png"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessage("Photo must be 2 MB or smaller");
    }

    @Test
    void updatePhoto_rejectsUnsupportedType() {
        assertThatThrownBy(() -> customerService.updatePhoto(1L, new byte[]{1}, "application/pdf"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessage("Photo must be a JPEG or PNG image");
    }

    @Test
    void updatePhoto_rejectsMissingContentType() {
        assertThatThrownBy(() -> customerService.updatePhoto(1L, new byte[]{1}, null))
                .isInstanceOf(InvalidPhotoException.class);
    }
}
