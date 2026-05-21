package com.boonya.lab.io.iot.controller;

import com.boonya.lab.io.iot.service.TimeSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final TimeSeriesService timeSeriesService;

    @GetMapping("/tdengine")
    public ResponseEntity<Map<String, Object>> checkTdengineHealth() {
        Map<String, Object> result = new HashMap<>();
        boolean healthy = timeSeriesService.isConnectionHealthy();
        result.put("healthy", healthy);
        result.put("timestamp", System.currentTimeMillis());

        return healthy ? ResponseEntity.ok(result) : ResponseEntity.status(503).body(result);
    }

}
