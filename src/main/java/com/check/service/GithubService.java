package com.check.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;

@Service
public class GithubService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GITHUB_API = "https://api.github.com";

    public void syncContent(String token, String repo, String path, String content, String message) {
        try {
            // 1. 验证仓库是否存在
            validateRepo(token, repo);
            
            // 获取文件 SHA（如果存在）
            String sha = null;
            try {
                sha = getFileSha(token, repo, path);
            } catch (Exception e) {
                // 文件不存在，继续创建
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            if (sha != null) {
                body.put("sha", sha);
            }

            String url = String.format("%s/repos/%s/contents/%s", GITHUB_API, repo, path);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("文件更新失败: " + response.getBody());
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("404")) {
                throw new RuntimeException("仓库或路径不存在，请检查仓库名称和访问权限");
            } else if (errorMessage.contains("401")) {
                throw new RuntimeException("Token无效或已过期，请检查Token设置");
            } else if (errorMessage.contains("403")) {
                throw new RuntimeException("没有仓库访问权限，请检查Token权限设置");
            }
            throw new RuntimeException("同步到 GitHub 失败: " + e.getMessage(), e);
        }
    }

    private String getFileSha(String token, String repo, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github.v3+json");

        String url = String.format("%s/repos/%s/contents/%s", GITHUB_API, repo, path);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                // 检查是否是文件而不是目录
                if (!root.isArray()) {
                    return root.get("sha").asText();
                }
            } catch (Exception e) {
                throw new RuntimeException("解析 GitHub 响应失败", e);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("404")) {
                return null;  // 文件不存在
            }
            throw e;
        }
        return null;  // 如果是目录或其他情况，返回 null
    }

    private void validateRepo(String token, String repo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github.v3+json");

        String url = String.format("%s/repos/%s", GITHUB_API, repo);
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("仓库不存在或无访问权限");
        }
    }

    private void createDirectory(String token, String repo, String path) {
        try {
            // 检查目录是否已存在
            String url = String.format("%s/repos/%s/contents/%s", GITHUB_API, repo, path);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Accept", "application/vnd.github.v3+json");
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );
            
            // 如果目录已存在，直接返回
            if (response.getStatusCode().is2xxSuccessful()) {
                return;
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("404")) {
                throw e;
            }
            // 目录不存在，创建一个空的 .gitkeep 文件
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("message", "Create directory: " + path);
            body.put("content", Base64.getEncoder().encodeToString(".".getBytes()));  // 创建一个包含点的文件

            String url = String.format("%s/repos/%s/contents/%s/.gitkeep", GITHUB_API, repo, path);
            restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class
            );
        }
    }
} 