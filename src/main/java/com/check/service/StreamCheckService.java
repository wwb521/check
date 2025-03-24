package com.check.service;

import com.check.model.CheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.io.File;

@Service
public class StreamCheckService {
    private static final int TIMEOUT_SECONDS = 2;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CheckResult checkStream(String url, String ffprobePath) {
        long startTime = System.currentTimeMillis();
        Process process = null;
        
        try {
            List<String> command = new ArrayList<>();
            command.add(ffprobePath);
            command.add("-v");
            command.add("quiet");  // 静默模式
            command.add("-print_format");
            command.add("json");
            command.add("-show_format");
            command.add("-show_streams");
            command.add("-i");
            command.add(url);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            final Process finalProcess = pb.start();
            process = finalProcess;

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(() -> {
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                return output.toString();
            });

            try {
                String result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                // 检查输出是否包含有效的JSON数据
                if (completed && !result.trim().isEmpty()) {
                    try {
                        JsonNode root = objectMapper.readTree(result);
                        
                        // 检查是否有流信息
                        if (root.has("streams") && root.get("streams").isArray() && 
                            root.get("streams").size() > 0) {
                            
                            // 获取第一个视频流
                            JsonNode videoStream = null;
                            for (JsonNode stream : root.get("streams")) {
                                if ("video".equals(stream.path("codec_type").asText())) {
                                    videoStream = stream;
                                    break;
                                }
                            }
                            
                            // 如果找到视频流，则认为是可用的
                            if (videoStream != null) {
                                String resolution = extractResolution(videoStream);
                                long responseTime = System.currentTimeMillis() - startTime;
                                
                                return CheckResult.builder()
                                        .available(true)
                                        .resolution(resolution)
                                        .responseTime(responseTime)
                                        .build();
                            }
                        }
                    } catch (Exception e) {
                        // JSON解析错误
                        return CheckResult.builder()
                                .available(false)
                                .error("解析流信息失败: " + e.getMessage())
                                .build();
                    }
                }
                
                return CheckResult.builder()
                        .available(false)
                        .error("无法获取流信息")
                        .build();
                
            } catch (TimeoutException e) {
                return CheckResult.builder()
                        .available(false)
                        .error("检测超时")
                        .build();
            } finally {
                executor.shutdownNow();
                if (process != null) {
                    process.destroyForcibly();
                }
            }
        } catch (Exception e) {
            return CheckResult.builder()
                    .available(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    private String extractResolution(JsonNode stream) {
        if (stream.has("width") && stream.has("height")) {
            int width = stream.get("width").asInt();
            int height = stream.get("height").asInt();
            if (width > 0 && height > 0) {
                return width + "x" + height;
            }
        }
        return "未知";
    }

    public boolean testFfprobe(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "-version");
            Process process = pb.start();
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String scanFfprobe() {
        // 常见的FFprobe安装路径
        String[] commonPaths = {
            "/usr/bin/ffprobe",
            "/usr/local/bin/ffprobe",
            "C:\\Program Files\\ffmpeg\\bin\\ffprobe.exe",
            "C:\\Program Files (x86)\\ffmpeg\\bin\\ffprobe.exe",
            "D:\\ffmpeg\\bin\\ffprobe.exe",
            System.getProperty("user.home") + "\\ffmpeg\\bin\\ffprobe.exe"
        };
        
        // 检查环境变量
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] pathDirs = pathEnv.split(File.pathSeparator);
            for (String dir : pathDirs) {
                String ffprobePath = new File(dir, isWindows() ? "ffprobe.exe" : "ffprobe").getAbsolutePath();
                if (testFfprobe(ffprobePath)) {
                    return ffprobePath;
                }
            }
        }
        
        // 检查常见路径
        for (String path : commonPaths) {
            if (testFfprobe(path)) {
                return path;
            }
        }
        
        return null;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
} 