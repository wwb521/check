package com.check.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CheckResult {
    private boolean available;
    private String resolution;
    private long responseTime;
    private String error;
} 