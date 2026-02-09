package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Comment;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.CommentRepository;
import com.turgo.barangayapp.Service.CommentService;
import com.turgo.barangayapp.Service.UserServices;
import com.turgo.barangayapp.dtos.FilterComment; // <--- THIS WAS MISSING
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private CommentService commentService;

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(@RequestBody FilterComment request, Authentication authentication) {
        // 1. Auth Check
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        try {
            // 2. Call the Service (Pass the DTO and the User)
            Comment savedComment = commentService.addComment(request, userOpt.get());
            return ResponseEntity.ok(savedComment);

        } catch (CommentService.ProfanityWarningException e) {
            // 3. Handle the "Warning" scenario
            // We return a 409 Conflict status so the Frontend knows to show the "Are you sure?" popup
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "PROFANITY_WARNING", // A code for your frontend to check
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            // It's helpful to print the stack trace for debugging if something else goes wrong
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
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