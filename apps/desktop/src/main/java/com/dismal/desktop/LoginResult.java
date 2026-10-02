package com.dismal.desktop;

public record LoginResult(UserMe user, String token) {
    public boolean isAllowed() {
        return user != null && ("ADMIN".equals(user.role())
                || "MANAGER".equals(user.role())
                || "OPERATOR".equals(user.role()));
    }
}
