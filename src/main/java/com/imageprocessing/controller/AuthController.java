package com.imageprocessing.controller;

import com.imageprocessing.model.AuthInfo;
import com.imageprocessing.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173, https://imagery.rishmi5h.com")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthInfo request) {
        log.info("Received registration request for user: {}", request.getUsername());
        try {
            String jwt = authService.register(request);
            if (jwt.chars().filter(ch -> ch == '.').count() != 2) {
                log.error("Invalid JWT token generated for user: {}", request.getUsername());
                throw new IllegalArgumentException("Invalid JWT token");
            }
            log.info("Successfully registered user: {}", request.getUsername());
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + jwt);
            return ResponseEntity.ok().headers(headers).body(jwt);
        } catch (IllegalArgumentException e) {
            log.error("Registration failed for user: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthInfo request) {
        log.info("Received login request for user: {}", request.getUsername());
        String jwt = authService.login(request);
        if (jwt.chars().filter(ch -> ch == '.').count() != 2) {
            log.error("Invalid JWT token generated for user: {}", request.getUsername());
            throw new IllegalArgumentException("Invalid JWT token");
        }
        log.info("Successfully logged in user: {}", request.getUsername());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return ResponseEntity.ok().headers(headers).body(jwt);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        log.info("Received token validation request");
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            boolean isValid = authService.validateToken(token);
            if (isValid) {
                log.info("Token validated successfully");
                return ResponseEntity.ok().body(Map.of("valid", true));
            } else {
                log.warn("Invalid token received");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
            }
        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
