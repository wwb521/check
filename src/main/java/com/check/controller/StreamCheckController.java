package com.check.controller;

import com.check.service.StreamCheckService;
import com.check.model.CheckResult;
import com.check.model.StreamCheckRequest;
import com.check.model.FfprobeTestRequest;
import com.check.model.TestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class StreamCheckController {

    @Autowired
    private StreamCheckService streamCheckService;

    @PostMapping("/check-stream")
    public ResponseEntity<CheckResult> checkStream(@RequestBody StreamCheckRequest request) {
        CheckResult result = streamCheckService.checkStream(request.getUrl(), request.getFfprobePath());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-ffprobe")
    public ResponseEntity<?> testFfprobe(@RequestBody FfprobeTestRequest request) {
        boolean isValid = streamCheckService.testFfprobe(request.getPath());
        if (isValid) {
            return ResponseEntity.ok().body(new TestResponse(true, "FFprobe 测试成功"));
        } else {
            return ResponseEntity.ok().body(new TestResponse(false, "FFprobe 路径无效"));
        }
    }

    @GetMapping("/scan-ffprobe")
    public ResponseEntity<?> scanFfprobe() {
        String ffprobePath = streamCheckService.scanFfprobe();
        if (ffprobePath != null) {
            return ResponseEntity.ok().body(new TestResponse(true, ffprobePath));
        } else {
            return ResponseEntity.ok().body(new TestResponse(false, "未找到FFprobe"));
        }
    }
} 