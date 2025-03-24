package com.check.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.check.service.StateService;
import javax.annotation.PostConstruct;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.check.model.GithubSyncRequest;
import com.check.service.GithubService;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class StreamController {
    private static final String FIXED_KEY = "playlist";
    private static final Map<String, String> contentStore = new ConcurrentHashMap<>();
    
    @Autowired
    private StateService stateService;
    @Autowired
    private GithubService githubService;

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

    @PostMapping("/generate-link")
    public ResponseEntity<String> generateLink(
        @RequestHeader("X-File-Format") String format,
        @RequestBody String content,
        HttpServletRequest request) {
        contentStore.put(FIXED_KEY + "_" + format, content);
        // 保存状态
        stateService.saveState("content_" + format, content);
        
        // 获取实际IP地址
        String serverAddress = request.getServerName();
        int serverPort = request.getServerPort();
        
        // 如果是localhost或本地地址，尝试获取本机实际IP
        if ("localhost".equalsIgnoreCase(serverAddress) || 
            "0:0:0:0:0:0:0:1".equals(serverAddress) || 
            "127.0.0.1".equals(serverAddress)) {
            try {
                // 获取所有网络接口
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                String wlanIp = null;
                String ethernetIp = null;
                String fallbackIp = null;

                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    // 过滤掉：关闭的接口、回环接口、虚拟接口
                    if (!iface.isUp() || iface.isLoopback()) {
                        continue;
                    }
                    
                    String ifaceName = iface.getDisplayName().toLowerCase();
                    // 过滤掉所有虚拟网卡
                    if (ifaceName.contains("vmware") || 
                        ifaceName.contains("virtual") || 
                        ifaceName.contains("vm") ||
                        ifaceName.contains("hyper-v") ||
                        ifaceName.contains("vpn")) {
                        continue;
                    }

                    // 遍历IP地址
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        // 只获取IPv4地址
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            String ip = addr.getHostAddress();
                            // 过滤掉特殊地址
                            if (!ip.startsWith("169.254.")) { // 过滤本地链接地址
                                // 根据网卡类型存储IP
                                if (ifaceName.contains("wlan") || ifaceName.contains("wireless") || ifaceName.contains("wifi")) {
                                    wlanIp = ip;
                                } else if (ifaceName.contains("ethernet") || ifaceName.contains("以太网")) {
                                    ethernetIp = ip;
                                } else if (fallbackIp == null) {
                                    fallbackIp = ip;
                                }
                            }
                        }
                    }
                }

                // 按优先级使用IP：无线网卡 > 有线网卡 > 其他网卡
                if (wlanIp != null) {
                    serverAddress = wlanIp;
                    logger.info("使用无线网卡IP: {}", wlanIp);
                } else if (ethernetIp != null) {
                    serverAddress = ethernetIp;
                    logger.info("使用有线网卡IP: {}", ethernetIp);
                } else if (fallbackIp != null) {
                    serverAddress = fallbackIp;
                    logger.info("使用其他网卡IP: {}", fallbackIp);
                }
            } catch (Exception e) {
                logger.warn("获取本机IP失败，使用原始地址: {}, 错误: {}", serverAddress, e.getMessage());
            }
        }
        
        return ResponseEntity.ok(String.format("http://%s:%d/api/stream-list/%s", serverAddress, serverPort, format));
    }

    @PostMapping("/update-content")
    public ResponseEntity<Void> updateContent(
        @RequestHeader("X-File-Format") String format,
        @RequestBody String content) {
        contentStore.put(FIXED_KEY + "_" + format, content);
        // 保存状态
        stateService.saveState("content_" + format, content);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stream-list/{format}")
    public ResponseEntity<String> getContent(@PathVariable String format) {
        String content = contentStore.get(FIXED_KEY + "_" + format);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/plain;charset=UTF-8"));
            String filename = String.format("filename=\"播放列表.%s\"", format);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; " + filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取内容失败");
        }
    }

    @PostMapping("/import-url")
    public ResponseEntity<String> importUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().body("URL不能为空");
        }

        try {
            URL fileUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            
            // 设置更多的请求头，模拟浏览器请求
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Pragma", "no-cache");
            conn.setRequestProperty("Referer", url);
            
            // 设置超时
            conn.setConnectTimeout(10000);  // 10秒连接超时
            conn.setReadTimeout(10000);     // 10秒读取超时
            
            // 允许重定向
            conn.setInstanceFollowRedirects(true);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                // 处理重定向
                String newUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                // 检查内容是否为空
                if (content.length() == 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("获取的内容为空");
                }
                
                return ResponseEntity.ok(content.toString());
            }
        } catch (Exception e) {
            // 提供更详细的错误信息
            String errorMessage = String.format("获取文件失败 [%s]: %s", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMessage);
        }
    }

    @RequestMapping(value = "/collect-url", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<String> collectUrl(
        @RequestParam(required = false) String url,
        @RequestBody(required = false) Map<String, String> request
    ) {
        // 优先使用查询参数中的 url，如果没有则尝试从请求体中获取
        if (url == null && request != null) {
            url = request.get("url");
        }
        
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().body("URL不能为空");
        }

        try {
            URL fileUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            // 设置请求头，模拟浏览器请求
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Pragma", "no-cache");
            
            // 增加超时时间
            conn.setConnectTimeout(10000);  // 10秒连接超时
            conn.setReadTimeout(10000);     // 10秒读取超时

            // 处理重定向
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("重定向地址无效");
                }
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
            }

            // 检查响应状态码
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return ResponseEntity.status(responseCode)
                    .body("请求失败: HTTP " + responseCode);
            }

            // 使用 try-with-resources 确保资源正确关闭
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                // 检查内容是否为空
                if (content.length() == 0) {
                    return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body("获取的内容为空");
                }
                
                return ResponseEntity.ok(content.toString());
            }
        } catch (SocketTimeoutException e) {
            logger.error("采集超时: " + url, e);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body("连接超时: " + e.getMessage());
        } catch (UnknownHostException e) {
            logger.error("无法解析主机: " + url, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("无法解析主机: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO异常: " + url, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO异常: " + e.getMessage());
        } catch (Exception e) {
            logger.error("采集异常: " + url, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("采集异常: " + e.getMessage());
        }
    }

    @PostMapping("/sync-github")
    public ResponseEntity<?> syncToGithub(@RequestBody GithubSyncRequest request) {
        try {
            githubService.syncContent(
                request.getToken(),
                request.getRepo(),
                request.getPath(),
                request.getContent(),
                request.getMessage()
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("同步失败: " + e.getMessage());
        }
    }

    // 添加启动时加载状态的方法
    @PostConstruct
    public void init() {
        stateService.loadState();
        String savedContent = (String) stateService.getState("content");
        if (savedContent != null) {
            contentStore.put(FIXED_KEY, savedContent);
        }
    }
} 