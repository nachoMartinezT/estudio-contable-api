package com.guidapixel.contable.mp.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpWebhookPayload {
    private String action;
    private MpData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MpData {
        private String id;
    }
}
