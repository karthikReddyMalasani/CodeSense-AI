package com.codesense.common.controller;

import com.codesense.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, String>>> root() {
        Map<String, String> status = Map.of(
            "status", "UP",
            "application", "CodeSense AI REST API",
            "version", "1.0.0",
            "frontendUrl", "http://localhost:3000"
        );
        return ResponseEntity.ok(ApiResponse.success("CodeSense AI API Server is running", status));
    }
}
