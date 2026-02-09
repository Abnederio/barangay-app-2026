package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Feedback;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.FeedbackService; // Import Service
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
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService; // Use Service

    @Autowired
    private UserServices userServices;

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        try {
            Feedback feedback = feedbackService.submitFeedback(request, userOpt.get());
            return ResponseEntity.ok(feedback);
        } catch (IllegalArgumentException e) {
            // Check specifically for the profanity flag
            if ("PROFANITY_DETECTED".equals(e.getMessage())) {
                return ResponseEntity.status(409).body(Map.of(
                        "error", "PROFANITY_WARNING",
                        "message", "Please remove the profane words from your feedback."
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/feedback")
    public ResponseEntity<?> getAllFeedback(Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }

    @PostMapping("/admin/feedback/{id}/reply")
    public ResponseEntity<?> replyToFeedback(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        String reply = request.get("reply");
        if (reply == null || reply.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reply cannot be empty"));
        }

        try {
            return ResponseEntity.ok(feedbackService.replyToFeedback(id, reply));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/admin/feedback/{id}")
    public ResponseEntity<?> deleteFeedback(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        if (feedbackService.deleteFeedback(id)) {
            return ResponseEntity.ok(Map.of("message", "Feedback deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        return userOpt.isPresent() && userOpt.get().isAdmin();
    }
}