package com.tynysai.notificationservice.model.enums;

public enum NotificationType {
    // Appointments
    APPOINTMENT_REQUESTED,
    APPOINTMENT_ACCEPTED,
    APPOINTMENT_REJECTED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_COMPLETED,

    // X-ray analyses
    XRAY_ASSIGNED,
    ANALYSIS_COMPLETED,
    ANALYSIS_REQUIRES_REVIEW,
    ANALYSIS_VALIDATED,

    // Diagnostic reports
    REPORT_READY,
    REPORT_UPDATED,

    // Lab results
    LAB_RESULT_ADDED,

    // Doctor approvals (admin-facing)
    DOCTOR_PENDING_APPROVAL,

    // Misc
    DOCTOR_MESSAGE,
    ACCOUNT_VERIFIED,
    SYSTEM
}