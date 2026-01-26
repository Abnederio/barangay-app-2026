package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.Official;
import com.turgo.barangayapp.Repository.OfficialRepository;
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
public class OfficialController {

    @Autowired
    private OfficialRepository officialRepository;
    
    @Autowired
    private UserServices userServices;

    @GetMapping("/public/officials")
    public ResponseEntity<List<Official>> getOfficials() {
        return ResponseEntity.ok(officialRepository.findByIsActiveTrueOrderByPositionAsc());
    }

    @PostMapping("/admin/officials")
    public ResponseEntity<?> createOfficial(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<com.turgo.barangayapp.Model.User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Official official = new Official();
        official.setName(request.get("name"));
        official.setPosition(request.get("position"));
        official.setEmail(request.get("email"));
        official.setPhoneNumber(request.get("phoneNumber"));
        if (request.containsKey("pictureUrl")) {
            official.setPictureUrl(request.get("pictureUrl"));
        }
        official.setActive(true);
        
        return ResponseEntity.ok(officialRepository.save(official));
    }

    @PutMapping("/admin/officials/{id}")
    public ResponseEntity<?> updateOfficial(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<com.turgo.barangayapp.Model.User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<Official> officialOpt = officialRepository.findById(id);
        if (officialOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Official official = officialOpt.get();
        if (request.containsKey("name")) official.setName(request.get("name"));
        if (request.containsKey("position")) official.setPosition(request.get("position"));
        if (request.containsKey("email")) official.setEmail(request.get("email"));
        if (request.containsKey("phoneNumber")) official.setPhoneNumber(request.get("phoneNumber"));
        if (request.containsKey("pictureUrl")) official.setPictureUrl(request.get("pictureUrl"));
        if (request.containsKey("isActive")) official.setActive(Boolean.parseBoolean(request.get("isActive")));
        
        return ResponseEntity.ok(officialRepository.save(official));
    }
}
