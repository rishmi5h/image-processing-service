package com.imageprocessing.service;

import com.imageprocessing.entities.Images;
import com.imageprocessing.entities.User;
import com.imageprocessing.repository.ImagesRepository;
import com.imageprocessing.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImageProcessingService {

    private final S3Client amazonS3Client;
    private final ImagesRepository imagesRepository;
    private final UserRepository userRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String bucketRegion;

    public Map<String, String> uploadImage(MultipartFile file, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        Integer userId = (Integer) authentication.getCredentials();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        PutObjectResponse putObjectResponse = amazonS3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Uploaded file: {}", putObjectResponse);
        String fileUrl = "https://" + bucketName + ".s3." + bucketRegion + ".amazonaws.com/" + fileName;

        Images uploadedImage = Images.builder()
                .s3Url(fileUrl)
                .fileName(fileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileExtension(fileExtension)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(Long.valueOf(userId))
                .build();

        imagesRepository.save(uploadedImage);

        Map<String, String> response = new HashMap<>();
        response.put("imageId", uploadedImage.getId().toString());
        response.put("fileName", fileName);
        response.put("fileUrl", fileUrl);
        response.put("fileType", file.getContentType());
        response.put("size", String.valueOf(file.getSize()));

        return response;
    }

    public Map<String, Object> getImageData(String id) {
        Images image = imagesRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        Map<String, Object> imageData = new HashMap<>();
        imageData.put("id", image.getId());
        imageData.put("s3Url", image.getS3Url());
        imageData.put("fileName", image.getFileName());
        imageData.put("fileType", image.getFileType());
        imageData.put("fileSize", image.getFileSize());
        imageData.put("fileExtension", image.getFileExtension());
        imageData.put("createdAt", image.getCreatedAt());
        imageData.put("updatedAt", image.getUpdatedAt());
        imageData.put("userId", image.getUserId());

        return imageData;
    }

    public void deleteImage(String id, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        Integer userId = (Integer) authentication.getCredentials();
        Images image = imagesRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        if (!image.getUserId().equals(Long.valueOf(userId))) {
            throw new SecurityException("User is not authorized to delete this image");
        }

        // Delete from S3
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(image.getFileName())
                .build();

        amazonS3Client.deleteObject(deleteObjectRequest);

        // Delete from MySQL
        imagesRepository.delete(image);
    }
}
