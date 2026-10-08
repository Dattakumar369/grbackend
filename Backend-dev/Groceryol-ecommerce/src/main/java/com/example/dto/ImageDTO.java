 package com.example.dto;

import lombok.Data;

@Data
public class ImageDTO {
    private String base64Data;
    private String contentType;
    private String altText;
}