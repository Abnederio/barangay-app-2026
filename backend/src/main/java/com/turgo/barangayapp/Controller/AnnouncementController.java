package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Announcement;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.AnnouncementRepository;
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
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserServices userServices;

    @GetMapping("/public/announcements")
    public ResponseEntity<List<Announcement>> getAnnouncements() {
        return ResponseEntity.ok(announcementRepository.findAllByOrderByCreatedAtDesc());
    }

    // CREATE
    @PostMapping("/admin/announcements")
    public ResponseEntity<?> createAnnouncement(@Valid @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.get("title"));
        announcement.setContent(request.get("content"));
        if (request.containsKey("imageUrl")) {
            announcement.setImageUrl(request.get("imageUrl"));
        }
        announcement.setCreatedBy(userOpt.get());

        return ResponseEntity.ok(announcementRepository.save(announcement));
    }

    // UPDATE (EDIT)
    @PutMapping("/admin/announcements/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody Map<String, String> request, Authentication authentication) {
        return announcementRepository.findById(id)
                .map(announcement -> {
                    announcement.setTitle(request.get("title"));
                    announcement.setContent(request.get("content"));
                    if (request.containsKey("imageUrl")) {
                        announcement.setImageUrl(request.get("imageUrl"));
                    }
                    return ResponseEntity.ok(announcementRepository.save(announcement));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/admin/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        return announcementRepository.findById(id)
                .map(announcement -> {
                    announcementRepository.delete(announcement);
                    return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}