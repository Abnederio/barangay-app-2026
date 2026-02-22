package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.ServiceApplication;
import com.turgo.barangayapp.Model.ServiceForm;
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

    // --- FETCH ALL SERVICES ---
    @GetMapping("/public/services")
    public ResponseEntity<List<com.turgo.barangayapp.Model.Service>> getAvailableServices() {
        // Changed to fetch all services (ongoing, finished, etc.)
        return ResponseEntity.ok(serviceRepository.findAllByOrderByNameAsc());
    }

    // --- APPLY FOR SERVICE (Includes Detailed Form) ---
    @PostMapping("/services/apply")
    public ResponseEntity<?> applyForService(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        // 1. Create the Application
        ServiceApplication application = new ServiceApplication();
        application.setServiceType(request.get("serviceType"));
        application.setAdditionalInfo(request.get("additionalInfo"));

        // 2. Create and populate the ServiceForm
        ServiceForm form = new ServiceForm();
        form.setFirstName(request.get("firstName"));
        form.setMiddleName(request.get("middleName"));
        form.setLastName(request.get("lastName"));
        form.setSuffix(request.get("suffix"));

        if (request.get("birthday") != null && !request.get("birthday").isEmpty()) {
            form.setBirthday(java.time.LocalDate.parse(request.get("birthday")));
        }

        form.setSex(request.get("sex"));
        form.setCivilStatus(request.get("civilStatus"));
        form.setAddress(request.get("address"));
        form.setContactNumber(request.get("contactNumber"));
        form.setEmailAddress(request.get("emailAddress"));
        form.setEducationalAttainment(request.get("educationalAttainment"));
        form.setOccupation(request.get("occupation"));
        form.setMonthlyIncome(request.get("monthlyIncome"));
        form.setPurokOrSitio(request.get("purokOrSitio"));

        if (request.containsKey("yearsOfResidency") && !request.get("yearsOfResidency").isEmpty()) {
            form.setYearsOfResidency(Integer.parseInt(request.get("yearsOfResidency")));
        }

        if (request.get("yearsOfResidency") != null && !request.get("yearsOfResidency").isEmpty()) {
            form.setYearsOfResidency(Integer.parseInt(request.get("yearsOfResidency")));
        }

        form.setPrecinctNumber(request.get("precinctNumber"));
        form.setPurpose(request.get("purpose"));
        form.setValidIdUrl(request.get("validIdUrl"));
        form.setEmergencyContactName(request.get("emergencyContactName"));
        form.setEmergencyContactNumber(request.get("emergencyContactNumber"));

        // 3. Attach the form to the application
        application.setServiceForm(form);

        // 4. Set User and Status
        application.setUser(userOpt.get());
        application.setStatus("PENDING");
        application.setNotificationSent(false);

        // 5. Save application (CascadeType.ALL will save the form too)
        ServiceApplication saved = serviceApplicationRepository.save(application);

        saved.setNotificationSent(true);
        serviceApplicationRepository.save(saved);

        return ResponseEntity.ok(Map.of(
                "message", "Service application submitted successfully.",
                "applicationId", saved.getId()
        ));
    }

    // --- GET APPLICATIONS (Resident View) ---
    @GetMapping("/services/my-applications")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(serviceApplicationRepository.findByUserIdOrderBySubmittedAtDesc(userOpt.get().getId()));
    }

    // --- GET ALL APPLICATIONS (Admin View) ---
    @GetMapping("/admin/services/applications")
    public ResponseEntity<?> getAllApplications(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        return ResponseEntity.ok(serviceApplicationRepository.findAllByOrderBySubmittedAtDesc());
    }

    // --- UPDATE APPLICATION STATUS (Admin Only) ---
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
        String newStatus = request.get("status");
        application.setStatus(newStatus);

        // Auto-add to service participants if approved
        if ("APPROVED".equals(newStatus)) {
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

    // --- CREATE SERVICE (Admin Only) ---
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

        // Use serviceStatus instead of isActive
        if (request.containsKey("serviceStatus")) {
            service.setServiceStatus(request.get("serviceStatus"));
        } else {
            service.setServiceStatus("ONGOING");
        }

        return ResponseEntity.ok(serviceRepository.save(service));
    }

    // --- UPDATE SERVICE (Admin Only) ---
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

        // Use serviceStatus instead of isActive
        if (request.containsKey("serviceStatus")) {
            service.setServiceStatus(request.get("serviceStatus"));
        }

        return ResponseEntity.ok(serviceRepository.save(service));
    }

    // --- DELETE SERVICE (Admin Only) ---
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

    // --- JOIN / LEAVE SERVICE directly (If still needed for some open services) ---
    @PostMapping("/services/{serviceId}/join")
    public ResponseEntity<?> joinService(@PathVariable Long serviceId, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userServices.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        Optional<com.turgo.barangayapp.Model.Service> serviceOpt = serviceRepository.findById(serviceId);

        // Changed check to use ServiceStatus
        if (serviceOpt.isEmpty() || "CANCELLED".equals(serviceOpt.get().getServiceStatus()) || "FINISHED".equals(serviceOpt.get().getServiceStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Service not found or is closed"));
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
}