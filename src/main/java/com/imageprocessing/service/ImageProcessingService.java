package com.imageprocessing.service;

import com.imageprocessing.entities.Images;
import com.imageprocessing.entities.User;
import com.imageprocessing.repository.ImagesRepository;
import com.imageprocessing.repository.UserRepository;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
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

    public Map<String, Object> transformImage(String id, Map<String, Object> transformations, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        Integer userId = (Integer) authentication.getCredentials();
        Images image = imagesRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        if (!image.getUserId().equals(Long.valueOf(userId))) {
            throw new SecurityException("User is not authorized to transform this image");
        }

        // Download the image from S3
        File tempFile = File.createTempFile("image", "." + image.getFileExtension());
        amazonS3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(image.getFileName()).build(), ResponseTransformer.toFile(tempFile));

        // Load the image using ImageJ
        ImagePlus img = IJ.openImage(tempFile.getAbsolutePath());
        ImageProcessor processor = img.getProcessor();

        // Apply transformations
        Map<String, Object> resize = (Map<String, Object>) transformations.get("resize");
        if (resize != null) {
            Integer width = (Integer) resize.get("width");
            Integer height = (Integer) resize.get("height");
            if (width != null && height != null) {
                processor = processor.resize(width, height);
            }
        }

        Map<String, Object> crop = (Map<String, Object>) transformations.get("crop");
        if (crop != null) {
            Integer cropX = (Integer) crop.get("x");
            Integer cropY = (Integer) crop.get("y");
            Integer cropWidth = (Integer) crop.get("width");
            Integer cropHeight = (Integer) crop.get("height");
            if (cropX != null && cropY != null && cropWidth != null && cropHeight != null) {
                processor.setRoi(cropX, cropY, cropWidth, cropHeight);
                processor = processor.crop();
            }
        }

        Integer rotate = (Integer) transformations.get("rotate");
        if (rotate != null) {
            processor.rotate(rotate);
        }

        String format = (String) transformations.get("format");

        Map<String, Object> filters = (Map<String, Object>) transformations.get("filters");
        if (filters != null) {
            Boolean grayscale = (Boolean) filters.get("grayscale");
            if (grayscale != null && grayscale) {
                processor = processor.convertToByte(true);
            }

            Boolean sepia = (Boolean) filters.get("sepia");
            if (sepia != null && sepia) {
                // Apply sepia filter logic here
            }
        }

        // Save the transformed image
        String transformedFileName = UUID.randomUUID().toString() + "." + (format != null ? format : image.getFileExtension());
        File transformedFile = new File(tempFile.getParent(), transformedFileName);
        IJ.save(new ImagePlus("", processor), transformedFile.getAbsolutePath());

        // Upload the transformed image to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(transformedFileName)
                .contentType("image/" + (format != null ? format : image.getFileExtension()))
                .build();
        amazonS3Client.putObject(putObjectRequest, RequestBody.fromFile(transformedFile));

        // Update the image metadata in the database
        Images transformedImage = Images.builder()
                .s3Url("https://" + bucketName + ".s3." + bucketRegion + ".amazonaws.com/" + transformedFileName)
                .fileName(transformedFileName)
                .fileType("image/" + (format != null ? format : image.getFileExtension()))
                .fileSize(transformedFile.length())
                .fileExtension(format != null ? format : image.getFileExtension())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(Long.valueOf(userId))
                .build();

        imagesRepository.save(transformedImage);

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("id", transformedImage.getId());
        response.put("s3Url", transformedImage.getS3Url());
        response.put("fileName", transformedImage.getFileName());
        response.put("fileType", transformedImage.getFileType());
        response.put("fileSize", transformedImage.getFileSize());
        response.put("fileExtension", transformedImage.getFileExtension());
        response.put("createdAt", transformedImage.getCreatedAt());
        response.put("updatedAt", transformedImage.getUpdatedAt());
        response.put("userId", transformedImage.getUserId());

        return response;
    }
}
