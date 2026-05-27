package com.revtalent.leave_service.dto;

import lombok.*;
import java.util.List;

/**
 * Response for GET /api/leaves/overlap
 *
 * hasOverlap   – true when the requested date range clashes with at least one
 *                existing APPLIED or APPROVED leave.
 * conflictingLeaves – the full details of each conflicting leave, so the
 *                     frontend can display helpful "You already have leave from
 *                     X to Y" messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveOverlapResponseDTO {
    private boolean hasOverlap;
    private List<LeaveRequestDTO> conflictingLeaves;
}
