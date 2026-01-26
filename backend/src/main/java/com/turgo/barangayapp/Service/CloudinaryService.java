package com.turgo.barangayapp.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
            "resource_type", "auto",
            "folder", "barangay-app"
        ));

        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // Extract public_id from URL
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            // Log error but don't throw - deletion is not critical
            System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
        }
    }

    private String extractPublicId(String imageUrl) {
        try {
            // Cloudinary URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            // or: https://res.cloudinary.com/{cloud_name}/image/upload/{folder}/{public_id}.{format}
            if (imageUrl.contains("cloudinary.com")) {
                String[] parts = imageUrl.split("/upload/");
                if (parts.length > 1) {
                    String path = parts[1];
                    // Remove version if present (v1234567890/)
                    if (path.matches("^v\\d+/.*")) {
                        path = path.substring(path.indexOf('/') + 1);
                    }
                    // Remove file extension
                    int lastDot = path.lastIndexOf('.');
                    if (lastDot > 0) {
                        path = path.substring(0, lastDot);
                    }
                    return path;
                }
            }
        } catch (Exception e) {
            // If extraction fails, return null
        }
        return null;
    }
}
