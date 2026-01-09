package com.filestorage.model;

import java.util.List;

public enum UserRole {
    OWNER,
    MANAGER,
    DEVELOPER,
    DESIGNER,
    TESTER,
    ANALYST,
    VIEWER;

    public static List<UserRole> getAll() {
        return List.of(UserRole.values());
    }
}
