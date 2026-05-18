package com.guidapixel.contable.auth.web;

import com.guidapixel.contable.auth.service.AuthService;
import com.guidapixel.contable.auth.web.dto.AuthenticationRequest;
import com.guidapixel.contable.auth.web.dto.AuthenticationResponse;
import com.guidapixel.contable.auth.web.dto.ChangePasswordRequest;
import com.guidapixel.contable.auth.web.dto.ForgotPasswordRequest;
import com.guidapixel.contable.auth.web.dto.ResetPasswordRequest;
import com.guidapixel.contable.auth.web.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    private Map<String, Object> errorResponse(Exception e) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ERROR");
        body.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        return body;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getUserProfile(auth.getName()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("status", "ERROR", "error", "No autenticado"));
            }
            return ResponseEntity.ok(service.changePassword(auth.getName(), request.getCurrentPassword(), request.getNewPassword()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "El email es obligatorio"));
            }
            return ResponseEntity.ok(service.forgotPassword(request.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(service.resetPassword(request.getToken(), request.getNewPassword()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }
}
