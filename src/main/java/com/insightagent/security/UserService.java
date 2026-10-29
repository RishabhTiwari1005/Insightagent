package com.insightagent.security;

import com.insightagent.domain.AppUser;
import com.insightagent.repo.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class UserService implements UserDetailsService {

    private static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9_.]{3,30}");

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }

    @Transactional
    public AppUser register(String username, String rawPassword) {
        String name = username == null ? "" : username.trim();
        if (!USERNAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Username must be 3 to 30 letters, numbers, dots or underscores");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (users.existsByUsername(name)) {
            throw new IllegalArgumentException("That username is taken");
        }
        return users.save(new AppUser(name, encoder.encode(rawPassword)));
    }
}
