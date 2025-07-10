package com.bytebites.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


public record AuthResponse(String token, String tokenType, String refreshToken, Long expiresIn, String role) {
}