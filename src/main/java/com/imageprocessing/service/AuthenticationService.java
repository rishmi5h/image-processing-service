package com.imageprocessing.service;

import com.imageprocessing.entities.User;
import com.imageprocessing.model.AuthInfo;
import com.imageprocessing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String register(AuthInfo request) {
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        repository.save(user);
        return jwtService.generateToken(user);
    }

    public String login(AuthInfo request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = repository.findByUsername(request.getUsername())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
            return jwtToken;
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Invalid username or password");
        }
    }

    public boolean validateToken(String token) {
        String username = jwtService.extractUsername(token);
        if (username != null) {
            User userDetails = repository.findByUsername(username).orElse(null);
            if (userDetails != null) {
                return jwtService.isTokenValid(token, userDetails);
            }
        }
        return false;
    }

    public void updatePassword(String username, String newPassword) {
        User user = repository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        log.info("Password updated successfully for user: {}", username);
    }
}
