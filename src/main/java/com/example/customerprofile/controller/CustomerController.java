package com.example.customerprofile.controller;

import com.example.customerprofile.dto.CustomerProfileResponse;
import com.example.customerprofile.dto.PhotoData;
import com.example.customerprofile.dto.UpdateCustomerRequest;
import com.example.customerprofile.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{id}")
    public CustomerProfileResponse getProfile(@PathVariable Long id) {
        return customerService.getProfile(id);
    }

    @PutMapping("/{id}")
    public CustomerProfileResponse updateProfile(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.updateProfile(id, request);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        PhotoData photo = customerService.getPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.content());
    }

    @PutMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePhoto(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file) throws IOException {
        customerService.updatePhoto(id, file.getBytes(), file.getContentType());
    }
}
