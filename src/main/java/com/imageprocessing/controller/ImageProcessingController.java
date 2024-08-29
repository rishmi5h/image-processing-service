package com.imageprocessing.controller;

import com.imageprocessing.model.LoginInfo;
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
    public ResponseEntity<String> register(@RequestBody LoginInfo request) {
        log.info("Register Info: {}", request.getUsername());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticate(@RequestBody LoginInfo request) {
        log.info("Login Info: {}", request.getUsername());
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
