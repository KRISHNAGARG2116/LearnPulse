package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@Tag(name = "Student Security Verification", description = "RBAC verification endpoints restricted to STUDENT role")
public class StudentSecurityTestController {

    @GetMapping("/test")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Student Endpoint Access Test", description = "Verifies that path-level /api/student/** and @PreAuthorize('hasRole(\"STUDENT\")') grant access to STUDENT users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testStudentAccess(@AuthenticationPrincipal User principal) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Access Granted: STUDENT security boundary verified");
        data.put("userId", principal.getId().toString());
        data.put("email", principal.getEmail());
        data.put("role", principal.getRole().name());
        data.put("authorities", principal.getAuthorities().toString());

        return ResponseEntity.ok(ApiResponse.success("STUDENT role access verified", data));
    }
}
