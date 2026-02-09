package com.turgo.barangayapp.dtos;

public class FilterComment {
    private String content;
    private String entityType; // e.g., "PROGRAM", "ANNOUNCEMENT"
    private Long entityId;
    private boolean confirmed; // This is the key for the "Warning" feature

    public FilterComment() {}

    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
}
