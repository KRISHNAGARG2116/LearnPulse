package com.learnpulse.backend.entity;

public enum Role {
    ADMIN,
    TEACHER,
    STUDENT;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
