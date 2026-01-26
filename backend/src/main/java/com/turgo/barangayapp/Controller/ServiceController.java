package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.ServiceApplication;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.ServiceApplicationRepository;
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
public class ServiceController {

    @Autowired
    private ServiceApplicationRepository serviceApplicationRepository;
    
    @Autowired
    private com.turgo.barangayapp.Repository.ServiceRepository serviceRepository;
    
    @Autowired
    private UserServices userServices;

    @GetMapping("/public/services")
    public ResponseEntity<List<com.turgo.barangayapp.Model.Service>> getAvailableServices() {
        return ResponseEntity.ok(serviceRepository.findByIsActiveTrueOrderByNameAsc());
    }

    @PostMapping("/services/{serviceId}/join")
    public ResponseEntity<?> joinService(@PathVariable Long serviceId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        Optional<com.turgo.barangayapp.Model.Service> serviceOpt = serviceRepository.findById(serviceId);
        if (serviceOpt.isEmpty() || !serviceOpt.get().isActive()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Service not found or inactive"));
        }
        
        com.turgo.barangayapp.Model.Service service = serviceOpt.get();
        User user = userOpt.get();
        if (service.getParticipants().contains(user)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already joined this service"));
        }
        service.getParticipants().add(user);
        serviceRepository.save(service);
        
        return ResponseEntity.ok(Map.of("message", "Successfully joined service"));
    }

    @PostMapping("/services/{serviceId}/leave")
    public ResponseEntity<?> leaveService(@PathVariable Long serviceId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        Optional<com.turgo.barangayapp.Model.Service> serviceOpt = serviceRepository.findById(serviceId);
        if (serviceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Service not found"));
        }
        
        com.turgo.barangayapp.Model.Service service = serviceOpt.get();
        User user = userOpt.get();
        if (!service.getParticipants().contains(user)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not a participant of this service"));
        }
        service.getParticipants().remove(user);
        serviceRepository.save(service);
        
        return ResponseEntity.ok(Map.of("message", "Successfully left service"));
    }

    @DeleteMapping("/admin/services/{serviceId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(@PathVariable Long serviceId, @PathVariable Long userId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> adminOpt = userServices.findByEmail(email);
        
        if (adminOpt.isEmpty() || !adminOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<com.turgo.barangayapp.Model.Service> serviceOpt = serviceRepository.findById(serviceId);
        Optional<User> userOpt = userServices.findById(userId);
        
        if (serviceOpt.isEmpty() || userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Service or user not found"));
        }
        
        com.turgo.barangayapp.Model.Service service = serviceOpt.get();
        User user = userOpt.get();
        service.getParticipants().remove(user);
        serviceRepository.save(service);
        
        return ResponseEntity.ok(Map.of("message", "Participant removed successfully"));
    }

    @PostMapping("/services/apply")
    public ResponseEntity<?> applyForService(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        ServiceApplication application = new ServiceApplication();
        application.setServiceType(request.get("serviceType"));
        application.setAdditionalInfo(request.get("additionalInfo"));
        application.setUser(userOpt.get());
        application.setStatus("PENDING");
        application.setNotificationSent(false);
        
        ServiceApplication saved = serviceApplicationRepository.save(application);
        
        // In a real app, you would send an email notification here
        // For now, we'll just mark it as notification sent
        saved.setNotificationSent(true);
        serviceApplicationRepository.save(saved);
        
        return ResponseEntity.ok(Map.of(
            "message", "Service application submitted successfully. You will be notified via email.",
            "applicationId", saved.getId()
        ));
    }

    @GetMapping("/services/my-applications")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        
        return ResponseEntity.ok(serviceApplicationRepository.findByUserIdOrderBySubmittedAtDesc(userOpt.get().getId()));
    }

    @GetMapping("/admin/services/applications")
    public ResponseEntity<?> getAllApplications(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        return ResponseEntity.ok(serviceApplicationRepository.findAllByOrderBySubmittedAtDesc());
    }

    @PutMapping("/admin/services/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<ServiceApplication> appOpt = serviceApplicationRepository.findById(id);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ServiceApplication application = appOpt.get();
        application.setStatus(request.get("status"));
        serviceApplicationRepository.save(application);
        
        return ResponseEntity.ok(Map.of("message", "Application status updated"));
    }

    @PostMapping("/admin/services")
    public ResponseEntity<?> createService(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        com.turgo.barangayapp.Model.Service service = new com.turgo.barangayapp.Model.Service();
        service.setName(request.get("name"));
        service.setDescription(request.get("description"));
        if (request.containsKey("imageUrl")) {
            service.setImageUrl(request.get("imageUrl"));
        }
        service.setActive(true);
        
        return ResponseEntity.ok(serviceRepository.save(service));
    }

    @PutMapping("/admin/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<com.turgo.barangayapp.Model.Service> serviceOpt = serviceRepository.findById(id);
        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        com.turgo.barangayapp.Model.Service service = serviceOpt.get();
        if (request.containsKey("name")) service.setName(request.get("name"));
        if (request.containsKey("description")) service.setDescription(request.get("description"));
        if (request.containsKey("imageUrl")) service.setImageUrl(request.get("imageUrl"));
        if (request.containsKey("isActive")) service.setActive(Boolean.parseBoolean(request.get("isActive")));
        
        return ResponseEntity.ok(serviceRepository.save(service));
    }

    @DeleteMapping("/admin/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);
        
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        serviceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Service deleted"));
    }
}
