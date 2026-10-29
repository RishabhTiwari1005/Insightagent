package com.insightagent.security;

import com.insightagent.domain.AppUser;
import com.insightagent.repo.AppUserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final AuthenticationContext auth;
    private final AppUserRepository users;

    public CurrentUser(AuthenticationContext auth, AppUserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    public AppUser get() {
        String name = auth.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElseThrow(() -> new IllegalStateException("Not logged in"));
        return users.findByUsername(name).orElseThrow();
    }

    public void logout() {
        auth.logout();
    }
}
