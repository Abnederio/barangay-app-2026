package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Program;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.ProgramService; // Import Service
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
public class ProgramController {

    @Autowired
    private ProgramService programService; // Uses Service now

    @Autowired
    private UserServices userServices;

    // --- PUBLIC ---
    @GetMapping("/public/programs")
    public ResponseEntity<List<Program>> getPrograms() {
        return ResponseEntity.ok(programService.getAllActivePrograms());
    }

    // --- PARTICIPANT ACTIONS ---
    @PostMapping("/programs/{programId}/join")
    public ResponseEntity<?> joinProgram(@PathVariable Long programId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "User not found"));

        String result = programService.joinProgram(programId, userOpt.get());

        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Successfully joined program"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
    }

    @PostMapping("/programs/{programId}/leave")
    public ResponseEntity<?> leaveProgram(@PathVariable Long programId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "User not found"));

        String result = programService.leaveProgram(programId, userOpt.get());

        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Successfully left program"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
    }

    // --- ADMIN ACTIONS ---

    @PostMapping("/admin/programs")
    public ResponseEntity<?> createProgram(@Valid @RequestBody Map<String, String> request, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        return ResponseEntity.ok(programService.createProgram(request));
    }

    @PutMapping("/admin/programs/{id}")
    public ResponseEntity<?> updateProgram(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        return programService.updateProgram(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/programs/{id}")
    public ResponseEntity<?> deleteProgram(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        if (programService.deleteProgram(id)) {
            return ResponseEntity.ok(Map.of("message", "Program deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/admin/programs/{programId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(@PathVariable Long programId, @PathVariable Long userId, Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        String result = programService.removeParticipant(programId, userId);

        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Participant removed successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
    }

    // Helper to check Admin
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        return userOpt.isPresent() && userOpt.get().isAdmin();
    }
}