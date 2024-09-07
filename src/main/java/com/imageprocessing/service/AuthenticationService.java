package com.imageprocessing.service;

import com.imageprocessing.entities.User;
import com.imageprocessing.model.AuthInfo;
import com.imageprocessing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
}
