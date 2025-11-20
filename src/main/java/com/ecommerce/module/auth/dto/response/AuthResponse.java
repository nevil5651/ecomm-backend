package com.ecommerce.module.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String email;
    private String role;
    private String fullName;
    private String refreshToken; // opaque token
}