package com.guidapixel.contable.mp.web;

import com.guidapixel.contable.mp.service.MpService;
import com.guidapixel.contable.mp.web.dto.CreatePaymentLinkRequest;
import com.guidapixel.contable.mp.web.dto.PaymentLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/mp")
@RequiredArgsConstructor
public class InternalMpController {

    private final MpService mpService;

    @PostMapping("/create-payment-link")
    public ResponseEntity<?> createPaymentLink(@RequestBody CreatePaymentLinkRequest request) {
        try {
            PaymentLinkResponse response = mpService.createPaymentLink(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
