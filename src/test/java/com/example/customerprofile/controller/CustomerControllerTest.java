package com.example.customerprofile.controller;

import com.example.customerprofile.dto.CustomerProfileResponse;
import com.example.customerprofile.dto.PhotoData;
import com.example.customerprofile.dto.UpdateCustomerRequest;
import com.example.customerprofile.exception.CustomerNotFoundException;
import com.example.customerprofile.exception.EmailAlreadyInUseException;
import com.example.customerprofile.exception.InvalidPhotoException;
import com.example.customerprofile.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void getProfile_returns200AndProfile() throws Exception {
        when(customerService.getProfile(1L)).thenReturn(
                new CustomerProfileResponse(1L, "Ada Lovelace", "ada@example.com", "/api/customers/1/photo"));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.photoUrl").value("/api/customers/1/photo"));
    }

    @Test
    void getProfile_returns404_whenCustomerDoesNotExist() throws Exception {
        when(customerService.getProfile(99L)).thenThrow(new CustomerNotFoundException(99L));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer 99 not found"));
    }

    @Test
    void updateProfile_returns200AndUpdatedProfile() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest("Ada King", "ada.king@example.com");
        when(customerService.updateProfile(1L, request)).thenReturn(
                new CustomerProfileResponse(1L, "Ada King", "ada.king@example.com", "/api/customers/1/photo"));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada King", "email": "ada.king@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada King"))
                .andExpect(jsonPath("$.email").value("ada.king@example.com"));
    }

    @Test
    void updateProfile_returns400_whenFieldsAreInvalid() throws Exception {
        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be a valid email address"));

        verifyNoInteractions(customerService);
    }

    @Test
    void updateProfile_returns409_whenEmailIsTaken() throws Exception {
        when(customerService.updateProfile(eq(1L), any(UpdateCustomerRequest.class)))
                .thenThrow(new EmailAlreadyInUseException("alan@example.com"));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "email": "alan@example.com"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getPhoto_returnsImageBytes() throws Exception {
        byte[] bytes = {1, 2, 3};
        when(customerService.getPhoto(1L)).thenReturn(new PhotoData(bytes, "image/png"));

        mockMvc.perform(get("/api/customers/1/photo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void updatePhoto_returns204() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ada.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/customers/1/photo").file(file))
                .andExpect(status().isNoContent());

        verify(customerService).updatePhoto(1L, new byte[]{1, 2, 3}, "image/png");
    }

    @Test
    void updatePhoto_returns400_whenPhotoIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1});
        doThrow(new InvalidPhotoException("Photo must be a JPEG or PNG image"))
                .when(customerService).updatePhoto(eq(1L), any(byte[].class), eq("application/pdf"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/customers/1/photo").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Photo must be a JPEG or PNG image"));
    }
}
