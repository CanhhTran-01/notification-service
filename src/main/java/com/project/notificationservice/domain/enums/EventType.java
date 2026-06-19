package com.project.notificationservice.domain.enums;

public enum EventType {

    // Authentication
    OTP_LOGIN,
    PASSWORD_RESET,
    ACCOUNT_VERIFIED,

    // System
    WELCOME,
    ACCOUNT_LOCKED,

    // External Service
    ORDER_CONFIRMED,

    // Default value
    GLOBAL
}
