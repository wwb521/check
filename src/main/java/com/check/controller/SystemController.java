package com.check.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class SystemController {
    
    @GetMapping("/local-ip")
    public ResponseEntity<Map<String, String>> getLocalIp() {
        Map<String, String> result = new HashMap<>();
        result.put("ip", "localhost");
        result.put("isp", "本地开发环境");
        return ResponseEntity.ok(result);
    }

    private Map<String, String> getIpInfo(String ip, String isp) {
        Map<String, String> result = new HashMap<>();
        result.put("ip", ip != null ? ip : "未找到有效IP");
        result.put("isp", isp);
        return result;
    }

    @PostMapping("/test-server")
    public ResponseEntity<Map<String, Object>> testServerConnection(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String ip = request.get("ip");
        
        if (ip == null || ip.isEmpty()) {
            response.put("success", false);
            response.put("message", "IP地址不能为空");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 验证IP地址格式
            if (!ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                response.put("success", false);
                response.put("message", "IP地址格式不正确");
                return ResponseEntity.ok(response);
            }
            
            // 尝试连接服务器
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = address.isReachable(5000); // 5秒超时
            
            response.put("success", reachable);
            response.put("message", reachable ? "服务器连接成功" : "服务器连接失败");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "服务器连接测试失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
} 