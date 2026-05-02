package com.guidapixel.contable.mp.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpPaymentDetail {
    private String id;
    private String status;
    private String statusDetail;
    private String preferenceId;
}
