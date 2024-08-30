package com.imageprocessing.controller;

import com.imageprocessing.model.AuthInfo;
import com.imageprocessing.model.AuthInfoResponse;
import com.imageprocessing.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<AuthInfo> register(@RequestBody AuthInfo request) {
        log.info("Register Info: {}", request.getUsername());
        String jwt = authService.register(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return ResponseEntity.ok().headers(headers).body(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthInfo> login(@RequestBody AuthInfo request) {
        log.info("Login Info: {}", request.getUsername());
        String jwt = authService.login(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return ResponseEntity.ok().headers(headers).body(request);
    }
}
