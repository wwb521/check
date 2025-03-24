package com.check.model;

import lombok.Data;

@Data
public class GithubSyncRequest {
    private String token;
    private String repo;
    private String path;
    private String content;
    private String message;
} 