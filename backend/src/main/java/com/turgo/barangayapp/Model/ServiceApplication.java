package com.turgo.barangayapp.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_applications")
public class ServiceApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String serviceType;
    
    @Column(columnDefinition = "TEXT")
    private String additionalInfo;
    
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private boolean notificationSent = false;

    public ServiceApplication() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}
