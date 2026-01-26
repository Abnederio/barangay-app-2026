package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Feedback;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.FeedbackRepository;
import com.turgo.barangayapp.Service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;
    
    @Autowired
    private UserServices userServices;

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        String message = request.get("message");
        if (message == null || message.trim().length() < 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "Feedback must be at least 10 characters"));
        }
        
        Feedback feedback = new Feedback();
        feedback.setMessage(message.trim());
        feedback.setUser(userOpt.get());
        
        return ResponseEntity.ok(feedbackRepository.save(feedback));
    }

    @GetMapping("/admin/feedback")
    public ResponseEntity<?> getAllFeedback(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        return ResponseEntity.ok(feedbackRepository.findAllByOrderBySubmittedAtDesc());
    }

    @PostMapping("/admin/feedback/{id}/reply")
    public ResponseEntity<?> replyToFeedback(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(id);
        if (feedbackOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Feedback feedback = feedbackOpt.get();
        String reply = request.get("reply");
        if (reply == null || reply.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reply cannot be empty"));
        }
        
        feedback.setAdminReply(reply.trim());
        feedback.setRepliedAt(LocalDateTime.now());
        feedback.setRead(true);
        
        return ResponseEntity.ok(feedbackRepository.save(feedback));
    }

    @DeleteMapping("/admin/feedback/{id}")
    public ResponseEntity<?> deleteFeedback(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        if (!feedbackRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        feedbackRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Feedback deleted successfully"));
    }
}
