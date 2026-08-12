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
@RequestMapping("/api/teacher")
@Tag(name = "Teacher Security Verification", description = "RBAC verification endpoints restricted to TEACHER role")
public class TeacherSecurityTestController {

    @GetMapping("/test")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Teacher Endpoint Access Test", description = "Verifies that path-level /api/teacher/** and @PreAuthorize('hasRole(\"TEACHER\")') grant access to TEACHER users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testTeacherAccess(@AuthenticationPrincipal User principal) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Access Granted: TEACHER security boundary verified");
        data.put("userId", principal.getId().toString());
        data.put("email", principal.getEmail());
        data.put("role", principal.getRole().name());
        data.put("authorities", principal.getAuthorities().toString());

        return ResponseEntity.ok(ApiResponse.success("TEACHER role access verified", data));
    }
}
