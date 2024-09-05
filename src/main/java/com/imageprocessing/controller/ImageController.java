package com.imageprocessing.controller;

import com.imageprocessing.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173, https://imagery.rishmi5h.com")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageProcessingService imageProcessingService;

    @PostMapping("/images")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> response = imageProcessingService.uploadImage(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/images/:id/transform")
    public String transform(@PathVariable String id) {
        return "transform";
    }

    @GetMapping("/images/:id")
    public String getImage(@PathVariable String id) {
        return "getImage";
    }

    @GetMapping("/images")
    public String downloadImage() {
        return "downloadImage";
    }



}
