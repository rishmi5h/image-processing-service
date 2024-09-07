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
        Integer userId = (Integer) authentication.getCredentials();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
}
