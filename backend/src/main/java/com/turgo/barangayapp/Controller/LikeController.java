package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Like;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.LikeRepository;
import com.turgo.barangayapp.Service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class LikeController {

    @Autowired
    private LikeRepository likeRepository;
    
    @Autowired
    private UserServices userServices;

    @PostMapping("/likes")
    public ResponseEntity<?> toggleLike(@RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        String entityType = request.get("entityType");
        String entityIdRaw = request.get("entityId");
        if (entityType == null || entityType.isBlank() || entityIdRaw == null || entityIdRaw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "entityType and entityId are required"));
        }

        final Long entityId;
        try {
            entityId = Long.parseLong(entityIdRaw);
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "entityId must be a number"));
        }

        entityType = entityType.trim().toUpperCase();
        
        Optional<Like> existingLike = likeRepository.findByUserIdAndEntityTypeAndEntityId(
            userOpt.get().getId(), entityType, entityId);
        
        if (existingLike.isPresent()) {
            // Unlike
            likeRepository.delete(existingLike.get());
            return ResponseEntity.ok(Map.of("liked", false, "message", "Unliked"));
        } else {
            // Like
            Like like = new Like();
            like.setUser(userOpt.get());
            like.setEntityType(entityType);
            like.setEntityId(entityId);
            likeRepository.save(like);
            return ResponseEntity.ok(Map.of("liked", true, "message", "Liked"));
        }
    }

    @GetMapping("/likes/{entityType}/{entityId}")
    public ResponseEntity<?> getLikes(@PathVariable String entityType, @PathVariable Long entityId) {
        entityType = entityType == null ? null : entityType.trim().toUpperCase();
        long count = likeRepository.countByEntityTypeAndEntityId(entityType, entityId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/likes/{entityType}/{entityId}/check")
    public ResponseEntity<?> checkUserLike(@PathVariable String entityType, @PathVariable Long entityId, Authentication authentication) {
        entityType = entityType == null ? null : entityType.trim().toUpperCase();
        if (authentication == null) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
        
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
        
        Optional<Like> like = likeRepository.findByUserIdAndEntityTypeAndEntityId(
            userOpt.get().getId(), entityType, entityId);
        
        return ResponseEntity.ok(Map.of("liked", like.isPresent()));
    }
}
