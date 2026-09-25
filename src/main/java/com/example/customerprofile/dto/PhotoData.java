package com.example.customerprofile.dto;

import java.io.Serializable;

// Serializable so it can be stored in Redis
public record PhotoData(byte[] content, String contentType) implements Serializable {
}
