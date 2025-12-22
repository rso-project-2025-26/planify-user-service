package com.planify.user_service.model;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean consentEmail;
    private Boolean consentSms;
}
