package com.guidapixel.contable.auth.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubscriptionRequest {
    private String moduleName;
    private boolean active;
    private List<String> modules;
}
