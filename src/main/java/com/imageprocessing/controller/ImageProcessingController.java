package com.imageprocessing.controller;

import com.imageprocessing.model.AuthInfoRequest;
import com.imageprocessing.model.AuthInfoResponse;
import com.imageprocessing.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthInfoResponse> register(@RequestBody AuthInfoRequest request) {
        log.info("Register Info: {}", request.getUsername());
        AuthInfoResponse response = new AuthInfoResponse();
        response.setUsername(request.getUsername());
        response.setPassword(request.getPassword());
        response.setJwt(authService.register(request));
        log.info("Register Response: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthInfoResponse> login(@RequestBody AuthInfoRequest request) {
        log.info("Login Info: {}", request.getUsername());
        AuthInfoResponse response = new AuthInfoResponse();
        response.setUsername(request.getUsername());
        response.setPassword(request.getPassword());
        response.setJwt(authService.login(request));
        log.info("Login Response: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }
}
