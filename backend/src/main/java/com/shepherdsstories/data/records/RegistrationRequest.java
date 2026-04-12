package com.shepherdsstories.data.records;

public record RegistrationRequest(
        String email,
        String role,          // MISSIONARY or SUPPORTER
        String authProvider,  // As of right now, only GOOGLE
        String displayName    // From your MissionarySignupForm
) {
}