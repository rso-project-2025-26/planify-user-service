package com.planify.user_service.model;

import lombok.Data;

@Data
public class Organization {
    private String name;
    private String slug;
    private String description;
    private String email;
    private Boolean business;
    private String password;
}
