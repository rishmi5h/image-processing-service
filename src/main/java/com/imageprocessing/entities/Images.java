package com.imageprocessing.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_images")
public class Images {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String s3Url;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileExtension;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
