package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Comment;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.CommentRepository;
import com.turgo.barangayapp.Service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserServices userServices;

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(@RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        Comment comment = new Comment();
        comment.setUser(userOpt.get());
        String entityType = request.get("entityType");
        if (entityType != null) {
            entityType = entityType.trim().toUpperCase();
        }
        comment.setEntityType(entityType);
        comment.setEntityId(Long.parseLong(request.get("entityId")));
        comment.setContent(request.get("content"));
        
        return ResponseEntity.ok(commentRepository.save(comment));
    }

    @GetMapping("/comments/{entityType}/{entityId}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable String entityType, @PathVariable Long entityId) {
        entityType = entityType == null ? null : entityType.trim().toUpperCase();
        return ResponseEntity.ok(commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        Optional<Comment> commentOpt = commentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Comment comment = commentOpt.get();
        // Only allow deletion if user is the comment author or an admin
        if (!comment.getUser().getId().equals(userOpt.get().getId()) && !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }
        
        commentRepository.delete(comment);
        return ResponseEntity.ok(Map.of("message", "Comment deleted"));
    }
}
