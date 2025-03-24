package com.check.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class NetworkController {
    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);
    private static final int CONNECT_TIMEOUT = 5000;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final int IPV6_CONNECT_TIMEOUT = 2000; // 减少IPv6连接超时时间到2秒
    private static final long IPV6_CACHE_DURATION_SUCCESS = 0; // 移除缓存
    private static final long IPV6_CACHE_DURATION_FAILURE = 0;  // 移除缓存
    private static final long IPV6_RATE_LIMIT = 0; // 移除频率限制
    private static Long lastIpv6CheckTime = null;
    private static Boolean lastIpv6Result = null;
    private static int checkCount = 0;
    private static final int MAX_CHECKS_PER_HOUR = 30; // 每小时最大检测次数
    private static long hourStartTime = System.currentTimeMillis();

    @GetMapping("/network-info")
    public Map<String, Object> getNetworkInfo(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 获取本地IP
            String localIp = getLocalIp();
            result.put("localIp", localIp);
            
            // 2. 优先尝试获取公网IP
            String metroIp = getMetroIp();
            if (metroIp == null || metroIp.isEmpty()) {
                // 3. 如果获取公网IP失败，再尝试从请求中获取
                metroIp = getRealIp(request);
            }
            result.put("metroIp", metroIp);
            
            if (metroIp != null) {
                String isp = getIspInfo(metroIp);
                result.put("isp", isp);
            }
            
            // 4. 检查IPv6支持
            boolean ipv6Support = checkIpv6Support();
            result.put("ipv6Support", ipv6Support);
            
            result.put("success", true);
        } catch (Exception e) {
            logger.error("获取网络信息失败: ", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    private String getLocalIp() throws SocketException {
        String wlanIp = null;
        String ethernetIp = null;
        String fallbackIp = null;

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            String name = networkInterface.getDisplayName().toLowerCase();
            
            // 跳过不活动或回环接口
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            
            // 跳过VMware虚拟网卡
            if (name.contains("vmware") || name.contains("virtual")) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    String ip = addr.getHostAddress();
                    
                    // 优先选择WLAN的IP
                    if (name.contains("wlan") || name.contains("wireless") || name.contains("wifi")) {
                        wlanIp = ip;
                    }
                    // 其次选择以太网的IP
                    else if (name.contains("ethernet") || name.contains("以太网")) {
                        ethernetIp = ip;
                    }
                    // 保存第一个找到的IP作为后备
                    else if (fallbackIp == null) {
                        fallbackIp = ip;
                    }
                }
            }
        }

        // 按优先级返回IP
        if (wlanIp != null) {
            logger.info("使用WLAN IP: {}", wlanIp);
            return wlanIp;
        }
        if (ethernetIp != null) {
            logger.info("使用以太网IP: {}", ethernetIp);
            return ethernetIp;
        }
        if (fallbackIp != null) {
            logger.info("使用其他网卡IP: {}", fallbackIp);
            return fallbackIp;
        }
        
        logger.warn("未找到有效的本地IP地址");
        return null;
    }

    private String getRealIp(HttpServletRequest request) {
        String ip = null;
        
        // 按优先级获取IP地址
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果是多级代理，获取第一个IP地址
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                logger.debug("获取到IP地址: {} 从请求头: {}", ip, header);
                break;
            }
        }
        
        // 如果没有获取到，使用远程地址
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            logger.debug("使用远程地址作为IP: {}", ip);
        }
        
        return ip;
    }

    private String getMetroIp() {
        try {
            // 尝试多个接口获取公网IP
            String[] ipApis = {
                "https://api.ipify.org?format=text",
                "https://api.myip.com",
                "https://api.ip.sb/ip",
                "https://api64.ipify.org?format=text",
                "https://checkip.amazonaws.com",
                "https://icanhazip.com",
                "https://ifconfig.me/ip"
            };
            
            for (String api : ipApis) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    headers.set("Accept", "text/plain,application/json");
                    
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<String> response = restTemplate.exchange(
                        api,
                        HttpMethod.GET,
                        entity,
                        String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        String ip = response.getBody().trim();
                        
                        // 如果是JSON响应，尝试解析
                        if (ip.startsWith("{")) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode root = mapper.readTree(ip);
                                if (root.has("ip")) {
                                    ip = root.get("ip").asText();
                                }
                            } catch (Exception e) {
                                continue;
                            }
                        }
                        
                        // 验证IP格式
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            logger.info("成功从 {} 获取公网IP: {}", api, ip);
                            return ip;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("从 {} 获取IP失败: {}", api, e.getMessage());
                    continue;
                }
            }
            logger.warn("所有公网IP获取接口均失败");
            return null;
        } catch (Exception e) {
            logger.error("获取公网IP失败: ", e);
            return null;
        }
    }

    private String getIspInfo(String ip) {
        try {
            // 尝试多个接口获取运营商信息
            String[] ispApis = {
                "http://myip.ipip.net/",
                "http://ip.cip.cc",
                "http://whois.pconline.com.cn/ipJson.jsp?ip=" + ip
            };
            
            for (String api : ispApis) {
                try {
                    String response = restTemplate.getForObject(api, String.class);
                    if (response != null) {
                        String isp = parseIspFromResponse(response, api);
                        if (isp != null && !isp.equals("未知")) {
                            return formatIspName(isp);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("使用{}获取运营商信息失败: {}", api, e.getMessage());
                    continue;
                }
            }
            return "未知";
        } catch (Exception e) {
            logger.error("获取运营商信息失败: ", e);
            return "未知";
        }
    }

    private String parseIspFromResponse(String response, String api) {
        try {
            if (api.contains("ipip.net")) {
                if (response.contains("来自于：")) {
                    String[] parts = response.split("来自于：");
                    if (parts.length > 1) {
                        String[] location = parts[1].trim().split("\\s+");
                        if (location.length > 3) {
                            return location[3];
                        }
                    }
                }
            } else if (api.contains("cip.cc")) {
                String[] lines = response.split("\\n");
                if (lines.length > 2) {
                    return lines[2].trim();
                }
            } else if (api.contains("pconline")) {
                // 从response中提取运营商信息
                int start = response.indexOf("\"addr\":\"") + 8;
                int end = response.indexOf("\"", start);
                if (start > 7 && end > start) {
                    String addr = response.substring(start, end);
                    String[] parts = addr.split("\\s+");
                    if (parts.length > 0) {
                        return parts[parts.length - 1];
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("解析运营商信息失败: {}", e.getMessage());
        }
        return "未知";
    }

    private String formatIspName(String isp) {
        // 标准化运营商名称
        if (isp == null || isp.trim().isEmpty()) {
            return "未知";
        }
        
        isp = isp.toLowerCase();
        if (isp.contains("mobile") || isp.contains("cmcc") || isp.contains("移动")) {
            return "中国移动";
        } else if (isp.contains("unicom") || isp.contains("联通")) {
            return "中国联通";
        } else if (isp.contains("telecom") || isp.contains("电信")) {
            return "中国电信";
        } else if (isp.contains("广电") || isp.contains("broadcast")) {
            return "中国广电";
        } else if (isp.contains("教育网") || isp.contains("cernet")) {
            return "中国教育网";
        } else if (isp.contains("铁通")) {
            return "中国铁通";
        } else if (isp.contains("鹏博士")) {
            return "鹏博士";
        } else if (isp.contains("广州") && isp.contains("数通")) {
            return "广州数通";
        } else if (isp.contains("长城") || isp.contains("gwbn")) {
            return "长城宽带";
        }
        
        // 如果是其他运营商，返回原始名称
        return isp;
    }

    private boolean checkIpv6Support() {
        long currentTime = System.currentTimeMillis();

        // 移除缓存检查逻辑，每次都重新检测
        boolean supported = false;
        try {
            // 1. 首先检查本地IPv6地址
            boolean hasIpv6Address = false;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                String name = networkInterface.getDisplayName().toLowerCase();
                
                // 只跳过虚拟网卡
                if (name.contains("vmware") || name.contains("virtual") || name.contains("hyper-v")) {
                    continue;
                }
                
                // 检查接口状态
                logger.info("检查网络接口: {}, 状态: up={}, loopback={}", 
                    name, networkInterface.isUp(), networkInterface.isLoopback());
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet6Address) {
                        Inet6Address ipv6Addr = (Inet6Address) addr;
                        String ipv6Address = addr.getHostAddress();
                        
                        // 记录所有IPv6地址的详细信息
                        logger.info("发现IPv6地址: {}, loopback={}, linkLocal={}, siteLocal={}, multicast={}", 
                            ipv6Address,
                            addr.isLoopbackAddress(),
                            ipv6Addr.isLinkLocalAddress(),
                            ipv6Addr.isSiteLocalAddress(),
                            addr.isMulticastAddress());
                        
                        // 放宽地址判断条件：接受全局单播地址、临时地址和ULA地址
                        if (!addr.isLoopbackAddress() && !addr.isMulticastAddress() && 
                            (ipv6Address.contains("temp") || ipv6Address.contains("fd") || 
                             !ipv6Addr.isLinkLocalAddress() || !ipv6Addr.isSiteLocalAddress())) {
                            logger.info("找到可用的IPv6地址: {}", ipv6Address);
                            hasIpv6Address = true;
                            break;
                        }
                    }
                }
                if (hasIpv6Address) break;
            }

            if (hasIpv6Address) {
                // 2. 如果有IPv6地址，尝试连接测试目标
                String[][] testTargets = {
                    {"Cloudflare-DNS", "2606:4700:4700::1111", "53"},   // Cloudflare DNS
                    {"Cloudflare-Web", "2606:4700:4700::1001", "443"},  // Cloudflare Web
                    {"阿里云-主", "2400:3200::1", "80"},               // 阿里云主节点
                    {"阿里云-备", "2400:3200:baba::1", "443"},        // 阿里云备用节点
                    {"谷歌-DNS", "2001:4860:4860::8888", "53"},       // Google DNS
                    {"谷歌-Web", "2404:6800:4008:c07::67", "443"}     // Google Web
                };

                // 尝试所有目标，任一成功即可
                for (String[] target : testTargets) {
                    try {
                        logger.info("尝试连接{}的IPv6地址: {}:{}", target[0], target[1], target[2]);
                        Socket socket = new Socket();
                        socket.connect(
                            new InetSocketAddress(target[1], Integer.parseInt(target[2])), 
                            IPV6_CONNECT_TIMEOUT
                        );
                        socket.close();
                        logger.info("成功连接到{}的IPv6地址", target[0]);
                        supported = true;
                        break;  // 一个成功就足够了
                    } catch (Exception e) {
                        logger.info("连接{}的IPv6地址失败: {}", target[0], e.getMessage());
                        // 继续尝试下一个目标
                    }
                }
            } else {
                logger.info("未找到可用的IPv6地址");
            }

            // 更新计数和时间
            lastIpv6CheckTime = currentTime;
            lastIpv6Result = supported;
            logger.info("IPv6检测最终结果: {}", supported ? "支持" : "不支持");
            
        } catch (Exception e) {
            logger.error("检查IPv6支持时发生错误: ", e);
            supported = false;
        }
        
        return supported;
    }

    @PostMapping("/check-url")
    public ResponseEntity<Map<String, Object>> checkUrlAccessibility(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        Map<String, Object> response = new HashMap<>();
        
        if (url == null || url.trim().isEmpty()) {
            response.put("accessible", false);
            response.put("error", "URL不能为空");
            return ResponseEntity.ok(response);
        }

        // 标准化 URL
        if (!url.toLowerCase().startsWith("http")) {
            url = "http://" + url;
        }

        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            
            // 对于m3u8等流媒体文件，使用GET请求而不是HEAD
            boolean isStreamingUrl = url.toLowerCase().endsWith(".m3u8") || 
                                   url.toLowerCase().endsWith(".ts") ||
                                   url.toLowerCase().contains("stream") ||
                                   url.toLowerCase().contains("live");
            
            connection.setRequestMethod(isStreamingUrl ? "GET" : "HEAD");
            connection.setConnectTimeout(10000);   // 增加到10秒连接超时
            connection.setReadTimeout(10000);      // 增加到10秒读取超时
            connection.setInstanceFollowRedirects(true);
            
            // 设置更完整的请求头
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            
            // 如果是流媒体URL，添加特定的请求头
            if (isStreamingUrl) {
                connection.setRequestProperty("Accept", "application/vnd.apple.mpegurl,application/x-mpegurl,*/*");
            }
            
            // 尝试建立连接
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            boolean isAccessible = responseCode >= 200 && responseCode < 400;
            
            response.put("accessible", isAccessible);
            response.put("statusCode", responseCode);
            
            // 如果是流媒体URL且访问成功，尝试读取一小部分内容以验证流的有效性
            if (isStreamingUrl && isAccessible) {
                try {
                    byte[] buffer = new byte[8192];
                    int bytesRead = connection.getInputStream().read(buffer);
                    if (bytesRead <= 0) {
                        response.put("accessible", false);
                        response.put("error", "流媒体内容为空");
                    }
                } catch (Exception e) {
                    response.put("accessible", false);
                    response.put("error", "无法读取流媒体内容: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (java.net.SocketTimeoutException e) {
            logger.error("检查URL可达性超时: " + url, e);
            response.put("accessible", false);
            response.put("error", "连接超时");
            response.put("errorType", "timeout");
            return ResponseEntity.ok(response);
            
        } catch (java.net.ConnectException e) {
            logger.error("检查URL可达性被拒绝: " + url, e);
            response.put("accessible", false);
            response.put("error", "连接被拒绝");
            response.put("errorType", "connection_refused");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查URL可达性失败: " + url, e);
            response.put("accessible", false);
            response.put("error", e.getMessage());
            response.put("errorType", "other");
            return ResponseEntity.ok(response);
            
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    logger.warn("关闭连接时发生错误", e);
                }
            }
        }
    }
}