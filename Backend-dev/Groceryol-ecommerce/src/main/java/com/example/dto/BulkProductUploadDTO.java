package com.example.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class BulkProductUploadDTO {
    private MultipartFile zipFile;
}