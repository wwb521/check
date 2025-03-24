package com.check.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestResponse {
    private boolean success;
    private String message;
} 