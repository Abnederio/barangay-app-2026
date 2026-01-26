package com.turgo.barangayapp.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"})
})
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String entityType; // "PROGRAM", "EVENT", "ANNOUNCEMENT"
    
    @Column(nullable = false)
    private Long entityId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Like() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
