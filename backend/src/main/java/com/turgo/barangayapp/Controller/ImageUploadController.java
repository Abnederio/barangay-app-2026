package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class ImageUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/admin/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Validate file size (10MB max)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size must be less than 10MB"));
            }

            String imageUrl = cloudinaryService.uploadImage(file);
            if (imageUrl == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload image"));
            }

            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }
}
