package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Announcement;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.AnnouncementService; // Import the Service
import com.turgo.barangayapp.Service.UserServices;
import jakarta.validation.Valid;
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
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService; // Use Service instead of Repository

    @Autowired
    private UserServices userServices;

    @GetMapping("/public/announcements")
    public ResponseEntity<List<Announcement>> getAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    // CREATE
    @PostMapping("/admin/announcements")
    public ResponseEntity<?> createAnnouncement(@Valid @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        // Logic is now in the service
        Announcement savedAnnouncement = announcementService.createAnnouncement(request, userOpt.get());
        return ResponseEntity.ok(savedAnnouncement);
    }

    // UPDATE (EDIT)
    @PutMapping("/admin/announcements/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody Map<String, String> request, Authentication authentication) {
        // (You can add the same admin check here if you want extra security)

        Optional<Announcement> updated = announcementService.updateAnnouncement(id, request);

        if (updated.isPresent()) {
            return ResponseEntity.ok(updated.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE
    @DeleteMapping("/admin/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        boolean deleted = announcementService.deleteAnnouncement(id);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}