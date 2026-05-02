package com.guidapixel.contable.notification.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private String templateType;
    private String toEmail;
    private String toName;
    private String tenantName;
    private Long tenantId;
    private Map<String, String> variables;
}
