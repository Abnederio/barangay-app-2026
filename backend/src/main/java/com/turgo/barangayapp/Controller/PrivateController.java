package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class PrivateController {

    @Autowired
    private UserServices userServices;

    @GetMapping("/user/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("userId", user.getId()); // Also include userId for compatibility
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("address", user.getAddress());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("isAdmin", user.isAdmin());
        
        return ResponseEntity.ok(profile);
    }
}
