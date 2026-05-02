package com.guidapixel.contable.auth.web;

import com.guidapixel.contable.auth.service.AuthService;
import com.guidapixel.contable.auth.web.dto.AuthenticationRequest;
import com.guidapixel.contable.auth.web.dto.AuthenticationResponse;
import com.guidapixel.contable.auth.web.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

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
}
