package com.tynysai.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalPatients;
    private long totalDoctors;
    private long activePatients;
    private long activeDoctors;
    private long pendingDoctorApprovals;
}
