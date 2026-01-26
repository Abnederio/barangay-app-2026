package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Event;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.EventRepository;
import com.turgo.barangayapp.Service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class EventController {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private UserServices userServices;

    @GetMapping("/public/events")
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        // Get all events, including past ones, sorted by eventDate descending (newest first)
        return ResponseEntity.ok(eventRepository.findAllByOrderByEventDateDesc());
    }

    @PostMapping("/admin/events")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Event event = new Event();
        event.setTitle(request.get("title"));
        event.setDescription(request.get("description"));
        event.setEventDate(LocalDateTime.parse(request.get("eventDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        event.setLocation(request.get("location"));
        if (request.containsKey("imageUrl")) {
            event.setImageUrl(request.get("imageUrl"));
        }
        event.setCreatedBy(userOpt.get());
        
        return ResponseEntity.ok(eventRepository.save(event));
    }

    @PutMapping("/admin/events/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<Event> eventOpt = eventRepository.findById(id);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Event event = eventOpt.get();
        if (request.containsKey("title")) event.setTitle(request.get("title"));
        if (request.containsKey("description")) event.setDescription(request.get("description"));
        if (request.containsKey("eventDate")) event.setEventDate(LocalDateTime.parse(request.get("eventDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (request.containsKey("location")) event.setLocation(request.get("location"));
        if (request.containsKey("imageUrl")) event.setImageUrl(request.get("imageUrl"));
        
        return ResponseEntity.ok(eventRepository.save(event));
    }

    @DeleteMapping("/admin/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        eventRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Event deleted"));
    }
}
