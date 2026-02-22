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
        return ResponseEntity.ok(serviceRepository.findAllByOrderByNameAsc());
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

        // In a real app, you trigger an email here. For now, we update the DB.
        saved.setNotificationSent(true);
        serviceApplicationRepository.save(saved);

        return ResponseEntity.ok(Map.of(
                "message", "Service application submitted successfully! You can track your status in the 'My Applications' tab.",
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
        String newStatus = request.get("status"); // APPROVED or REJECTED
        application.setStatus(newStatus);

        // If approved, automatically add them to the Service participants list
        if ("APPROVED".equals(newStatus)) {
            // Find the actual service by name (serviceType)
            serviceRepository.findAllByOrderByNameAsc().stream()
                    .filter(s -> s.getName().equals(application.getServiceType()))
                    .findFirst()
                    .ifPresent(service -> {
                        service.getParticipants().add(application.getUser());
                        serviceRepository.save(service);
                    });
        }

        serviceApplicationRepository.save(application);

        return ResponseEntity.ok(Map.of("message", "Application status updated to " + newStatus));
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
        if (request.containsKey("imageUrl")) service.setImageUrl(request.get("imageUrl"));
        if (request.containsKey("serviceStatus")) service.setServiceStatus(request.get("serviceStatus"));

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
        if (request.containsKey("serviceStatus")) service.setServiceStatus(request.get("serviceStatus"));

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