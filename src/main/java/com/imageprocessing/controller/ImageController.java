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
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@CrossOrigin(origins = "http://localhost:5173, https://imagery.rishmi5h.com")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

     private final ImageProcessingService imageProcessingService;

    @PostMapping("/images")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        log.info("Received image upload request from user: {}", authentication.getName());
        try {
            Map<String, String> response = imageProcessingService.uploadImage(file, authentication);
            log.info("Successfully uploaded image for user: {}", authentication.getName());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload image for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/images/{id}/transform")
    public ResponseEntity<?> transformImage(
            @PathVariable String id,
            @RequestBody Map<String, Object> transformations,
            Authentication authentication) {
        log.info("Received image transformation request for image ID: {} from user: {}", id, authentication.getName());
        try {
            Map<String, Object> response = imageProcessingService.transformImage(id, transformations, authentication);
            log.info("Successfully transformed image ID: {} for user: {}", id, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Image not found for ID: {} requested by user: {}", id, authentication.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Unauthorized access attempt to image ID: {} by user: {}", id, authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to transform image ID: {} for user: {}", id, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to transform image: " + e.getMessage()));
        }
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<?> getImage(@PathVariable String id) {
        log.info("Received request to get image data for ID: {}", id);
        try {
            Map<String, Object> imageData = imageProcessingService.getImageData(id);
            log.info("Successfully retrieved image data for ID: {}", id);
            return ResponseEntity.ok(imageData);
        } catch (IllegalArgumentException e) {
            log.warn("Image not found for ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve image data for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve image data"));
        }
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable String id, Authentication authentication) {
        log.info("Received request to delete image ID: {} from user: {}", id, authentication.getName());
        try {
            imageProcessingService.deleteImage(id, authentication);
            log.info("Successfully deleted image ID: {} for user: {}", id, authentication.getName());
            return ResponseEntity.ok().body(Map.of("message", "Image deleted successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("Image not found for deletion, ID: {} requested by user: {}", id, authentication.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Unauthorized deletion attempt for image ID: {} by user: {}", id, authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete image ID: {} for user: {}", id, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete image: " + e.getMessage()));
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertImage(
            @RequestBody MultipartFile file,
            @RequestParam("format") String format) {
        log.info("Received image conversion request to format: {}", format);
        try {
            byte[] convertedImage = imageProcessingService.convertImage(file, format);
            log.info("Successfully converted image to format: {}", format);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("image/" + format));
            headers.setContentDispositionFormData("attachment", "converted_image." + format);
            return new ResponseEntity<>(convertedImage, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image conversion request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to convert image to format: {}", format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to convert image: " + e.getMessage()));
        }
    }
}