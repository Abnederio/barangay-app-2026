package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Program;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.ProgramRepository;
import com.turgo.barangayapp.Service.UserServices;
import jakarta.validation.Valid;
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
public class ProgramController {

    @Autowired
    private ProgramRepository programRepository;
    
    @Autowired
    private UserServices userServices;

    @GetMapping("/public/programs")
    public ResponseEntity<List<Program>> getPrograms() {
        return ResponseEntity.ok(programRepository.findByIsActiveTrueOrderByStartDateAsc());
    }

    @PostMapping("/programs/{programId}/join")
    public ResponseEntity<?> joinProgram(@PathVariable Long programId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        Optional<Program> programOpt = programRepository.findById(programId);
        if (programOpt.isEmpty() || !programOpt.get().isActive()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Program not found or inactive"));
        }
        
        Program program = programOpt.get();
        User user = userOpt.get();
        if (program.getParticipants().contains(user)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already joined this program"));
        }
        program.getParticipants().add(user);
        programRepository.save(program);
        
        return ResponseEntity.ok(Map.of("message", "Successfully joined program"));
    }

    @PostMapping("/programs/{programId}/leave")
    public ResponseEntity<?> leaveProgram(@PathVariable Long programId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        Optional<Program> programOpt = programRepository.findById(programId);
        if (programOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Program not found"));
        }
        
        Program program = programOpt.get();
        User user = userOpt.get();
        if (!program.getParticipants().contains(user)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not a participant of this program"));
        }
        program.getParticipants().remove(user);
        programRepository.save(program);
        
        return ResponseEntity.ok(Map.of("message", "Successfully left program"));
    }

    @DeleteMapping("/admin/programs/{programId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(@PathVariable Long programId, @PathVariable Long userId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> adminOpt = userServices.findByEmail(email);
        
        if (adminOpt.isEmpty() || !adminOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<Program> programOpt = programRepository.findById(programId);
        Optional<User> userOpt = userServices.findById(userId);
        
        if (programOpt.isEmpty() || userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Program or user not found"));
        }
        
        Program program = programOpt.get();
        User user = userOpt.get();
        program.getParticipants().remove(user);
        programRepository.save(program);
        
        return ResponseEntity.ok(Map.of("message", "Participant removed successfully"));
    }

    @PostMapping("/admin/programs")
    public ResponseEntity<?> createProgram(@Valid @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Program program = new Program();
        program.setName(request.get("name"));
        program.setDescription(request.get("description"));
        program.setStartDate(LocalDateTime.parse(request.get("startDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        program.setEndDate(LocalDateTime.parse(request.get("endDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        program.setActive(true);
        if (request.containsKey("imageUrl")) {
            program.setImageUrl(request.get("imageUrl"));
        }
        
        return ResponseEntity.ok(programRepository.save(program));
    }

    @PutMapping("/admin/programs/{id}")
    public ResponseEntity<?> updateProgram(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<Program> programOpt = programRepository.findById(id);
        if (programOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Program program = programOpt.get();
        if (request.containsKey("name")) program.setName(request.get("name"));
        if (request.containsKey("description")) program.setDescription(request.get("description"));
        if (request.containsKey("startDate")) program.setStartDate(LocalDateTime.parse(request.get("startDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (request.containsKey("endDate")) program.setEndDate(LocalDateTime.parse(request.get("endDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (request.containsKey("isActive")) program.setActive(Boolean.parseBoolean(request.get("isActive")));
        if (request.containsKey("imageUrl")) program.setImageUrl(request.get("imageUrl"));
        
        return ResponseEntity.ok(programRepository.save(program));
    }

    @DeleteMapping("/admin/programs/{id}")
    public ResponseEntity<?> deleteProgram(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        programRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Program deleted"));
    }
}
