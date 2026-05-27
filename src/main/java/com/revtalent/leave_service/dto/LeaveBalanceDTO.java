package com.revtalent.leave_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveBalanceDTO {
    private String type;
    private int used;
    private int total;
}