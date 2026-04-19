package com.tynysai.notificationservice.model.enums;

/**
 * Notification codes. The server stores only the code and a map of parameters;
 * the client resolves the localized title/message from its own i18n dictionary
 * using the code as the key.
 */
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

    // Misc
    DOCTOR_MESSAGE,
    ACCOUNT_VERIFIED,
    SYSTEM
}