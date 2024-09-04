package com.imageprocessing.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImageProcessingService {

//    private final AmazonS3 amazonS3Client;
//
//    @Value("${aws.s3.bucket}")
//    private String bucketName;
//
//    public Map<String, String> uploadImage(MultipartFile file) throws IOException {
//        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setContentType(file.getContentType());
//        metadata.setContentLength(file.getSize());
//
//        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata);
//        amazonS3Client.putObject(putObjectRequest);
//
//        String fileUrl = amazonS3Client.getUrl(bucketName, fileName).toString();
//
//        Map<String, String> response = new HashMap<>();
//        response.put("fileName", fileName);
//        response.put("fileUrl", fileUrl);
//        response.put("fileType", file.getContentType());
//        response.put("size", String.valueOf(file.getSize()));
//
//        return response;
//    }
}
