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
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import java.time.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;

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
        log.info("Starting image upload process for user: {}", authentication.getName());
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated user attempted to upload an image");
            throw new SecurityException("User is not authenticated");
        }

        Integer userId = (Integer) authentication.getCredentials();
        log.debug("Fetching user details for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", userId);
                    return new SecurityException("User not found");
                });

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        log.info("Preparing to upload file: {}, with extension: {}", fileName, fileExtension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        log.debug("Uploading file to S3: {}", fileName);
        PutObjectResponse putObjectResponse = amazonS3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("File uploaded successfully: {}", putObjectResponse);
        String fileUrl = "https://" + bucketName + ".s3." + bucketRegion + ".amazonaws.com/" + fileName;

        log.debug("Creating image record in database");
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
        log.info("Image record created in database with id: {}", uploadedImage.getId());

        Map<String, String> response = new HashMap<>();
        response.put("imageId", uploadedImage.getId().toString());
        response.put("fileName", fileName);
        response.put("fileUrl", fileUrl);
        response.put("fileType", file.getContentType());
        response.put("size", String.valueOf(file.getSize()));

        log.info("Image upload process completed successfully for user: {}", authentication.getName());
        return response;
    }

    public Map<String, Object> getImageData(String id) {
        log.info("Fetching image data for id: {}", id);
        Images image = imagesRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> {
                    log.error("Image not found for id: {}", id);
                    return new IllegalArgumentException("Image not found");
                });

        log.debug("Image found, preparing response");
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

        log.info("Image data fetched successfully for id: {}", id);
        return imageData;
    }

    public void deleteImage(String id, Authentication authentication) throws IOException {
        log.info("Starting image deletion process for id: {} by user: {}", id, authentication.getName());
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated user attempted to delete an image");
            throw new SecurityException("User is not authenticated");
        }

        Integer userId = (Integer) authentication.getCredentials();
        log.debug("Fetching image details for id: {}", id);
        Images image = imagesRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> {
                    log.error("Image not found for id: {}", id);
                    return new IllegalArgumentException("Image not found");
                });

        if (!image.getUserId().equals(Long.valueOf(userId))) {
            log.error("User {} is not authorized to delete image {}", userId, id);
            throw new SecurityException("User is not authorized to delete this image");
        }

        log.debug("Deleting image from S3: {}", image.getFileName());
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(image.getFileName())
                .build();

        amazonS3Client.deleteObject(deleteObjectRequest);
        log.info("Image deleted from S3: {}", image.getFileName());

        log.debug("Deleting image record from database");
        imagesRepository.delete(image);
        log.info("Image deletion process completed successfully for id: {}", id);
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
        try {
            log.info("Attempting to download image from S3: bucket={}, key={}", bucketName, image.getFileName());
            amazonS3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(image.getFileName()).build(), ResponseTransformer.toFile(tempFile));
            log.info("Successfully downloaded image from S3 to {}", tempFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to download image from S3", e);
            throw new IOException("Failed to download image from S3", e);
        }

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
                applySepiaFilter(processor);
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

    private void applySepiaFilter(ImageProcessor processor) {
        int width = processor.getWidth();
        int height = processor.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] rgb = processor.getPixel(x, y, (int[]) null);

                int tr = (int) (0.393 * rgb[0] + 0.769 * rgb[1] + 0.189 * rgb[2]);
                int tg = (int) (0.349 * rgb[0] + 0.686 * rgb[1] + 0.168 * rgb[2]);
                int tb = (int) (0.272 * rgb[0] + 0.534 * rgb[1] + 0.131 * rgb[2]);

                rgb[0] = Math.min(255, tr);
                rgb[1] = Math.min(255, tg);
                rgb[2] = Math.min(255, tb);

                processor.putPixel(x, y, rgb);
            }
        }
    }

    public byte[] convertImage(MultipartFile file, String format) throws IOException {
        log.info("Starting image conversion to format: {}", format);
        
        // Check if the file is an image and has a supported format
        String contentType = file.getContentType();
        if (contentType == null || !isSupportedImageFormat(contentType)) {
            log.error("Unsupported image format. Content type: {}", contentType);
            throw new IllegalArgumentException("Unsupported image format. Supported formats are JPEG, JPG, PNG, GIF, BMP, WBMP, and WebP.");
        }

        // Read the input image
        BufferedImage inputImage = null;
        try {
            inputImage = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            log.error("Failed to read input image", e);
            throw new IOException("Failed to read input image. The file might be corrupted.", e);
        }

        if (inputImage == null) {
            log.error("Failed to read image. The image might be corrupted or in an unsupported format.");
            throw new IOException("Failed to read image. The image might be corrupted or in an unsupported format.");
        }

        // Convert the image
        BufferedImage convertedImage = new BufferedImage(
                inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImage.createGraphics().drawImage(inputImage, 0, 0, Color.WHITE, null);

        // Save the converted image to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            boolean success = ImageIO.write(convertedImage, format, baos);
            if (!success) {
                log.error("No appropriate writer found for format: {}", format);
                throw new IOException("Failed to write image in the specified format: " + format);
            }
        } catch (IOException e) {
            log.error("Failed to write converted image", e);
            throw new IOException("Failed to write converted image", e);
        }

        log.info("Image conversion completed successfully");
        return baos.toByteArray();
    }

    private boolean isSupportedImageFormat(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/bmp") ||
               contentType.equals("image/wbmp") ||
               contentType.equals("image/webp");
    }
}