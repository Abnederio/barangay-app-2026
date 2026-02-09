package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Event;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.EventService; // Import Service
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
public class EventController {

    @Autowired
    private EventService eventService; // Use Service

    @Autowired
    private UserServices userServices;

    // GET Public (Filtered by Date)
    @GetMapping("/public/events")
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    // CREATE
    @PostMapping("/admin/events")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, String> request, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        String email = authentication.getName();
        User admin = userServices.findByEmail(email).get(); // Safe because isAdmin checked it

        return ResponseEntity.ok(eventService.createEvent(request, admin));
    }

    // UPDATE
    @PutMapping("/admin/events/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        return eventService.updateEvent(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/admin/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        if (eventService.deleteEvent(id)) {
            return ResponseEntity.ok(Map.of("message", "Event deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    // Helper to check Admin
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        return userOpt.isPresent() && userOpt.get().isAdmin();
    }
}