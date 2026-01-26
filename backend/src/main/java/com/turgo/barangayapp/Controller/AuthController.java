package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Config.JwtUtil;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.UserServices;
import com.turgo.barangayapp.dtos.AuthResponse;
import com.turgo.barangayapp.dtos.ForgotPasswordRequest;
import com.turgo.barangayapp.dtos.LoginRequest;
import com.turgo.barangayapp.dtos.ResetPasswordRequest;
import com.turgo.barangayapp.dtos.SecurityQuestionResponse;
import com.turgo.barangayapp.dtos.SignupRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserServices userServices;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        Map<String, String> errors = new HashMap<>();

        // Normalize email to avoid case/whitespace login issues
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        
        if (userServices.existsByEmail(normalizedEmail)) {
            errors.put("email", "Email already exists");
            return ResponseEntity.badRequest().body(errors);
        }
        
        // Check if this is the first user (will be admin) or if admin code is provided
        boolean isFirstUser = userServices.findAll().isEmpty();
        String adminCode = request.getAdminCode();
        boolean isAdmin = isFirstUser || (adminCode != null && "ADMIN2024".equals(adminCode.trim()));
        
        User user = userServices.createUser(
            normalizedEmail,
            request.getPassword(),
            request.getFullName(),
            request.getAddress(),
            request.getPhoneNumber(),
            isAdmin,
            request.getSecurityQuestion(),
            request.getSecurityAnswer()
        );
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.isAdmin());
        AuthResponse response = new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.isAdmin());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Map<String, String> errors = new HashMap<>();

        // Normalize email to avoid case/whitespace login issues
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        
        User user = userServices.findByEmail(normalizedEmail)
            .orElse(null);
        
        if (user == null || !userServices.validatePassword(request.getPassword(), user.getPassword())) {
            errors.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.isAdmin());
        AuthResponse response = new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.isAdmin());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/forgot-password/question")
    public ResponseEntity<?> getSecurityQuestion(@RequestParam String email) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        Optional<User> userOpt = userServices.findByEmail(normalizedEmail);
        
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not for security
            return ResponseEntity.ok(new SecurityQuestionResponse(""));
        }
        
        User user = userOpt.get();
        return ResponseEntity.ok(new SecurityQuestionResponse(user.getSecurityQuestion()));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifySecurityAnswer(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userServices.findByEmail(normalizedEmail);
        
        if (userOpt.isEmpty()) {
            errors.put("error", "Invalid email or security answer");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
        
        User user = userOpt.get();
        if (!userServices.validateSecurityAnswer(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            errors.put("error", "Invalid email or security answer");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
        
        // Return success - user can now reset password
        return ResponseEntity.ok(Map.of("message", "Security answer verified. You can now reset your password."));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userServices.findByEmail(normalizedEmail);
        
        if (userOpt.isEmpty()) {
            errors.put("error", "Invalid email or security answer");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
        
        User user = userOpt.get();
        if (!userServices.validateSecurityAnswer(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            errors.put("error", "Invalid email or security answer");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
        
        // Update password
        userServices.updatePassword(user, request.getNewPassword());
        
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now login with your new password."));
    }
}
