package tech.justjava.process_manager.gallery;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "file_info")
public class FileInfo {

    @Id
    @Column(name = "uuid_id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "microservice_file_id", nullable = false)
    private String microserviceFileId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "case_number")
    private String caseNumber;

    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status", nullable = false)
    private String status; // "TEMPORARY" or "COMMITTED"

    @Column(name = "session_id")
    private String sessionId; // For temporary files, track which session they belong to

    // Default constructor
    public FileInfo() {
        LocalDateTime now = LocalDateTime.now();
        this.dateAdded = now;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = "TEMPORARY";
    }

    // Constructor with basic fields
    public FileInfo(String id, String name, String microserviceFileId, String type, String size) {
        this();
        this.id = id;
        this.name = name;
        this.microserviceFileId = microserviceFileId;
        this.type = type;
        this.size = size;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (dateAdded == null) {
            dateAdded = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMicroserviceFileId() {
        return microserviceFileId;
    }

    public void setMicroserviceFileId(String microserviceFileId) {
        this.microserviceFileId = microserviceFileId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // Helper method for backward compatibility with the frontend
    public String getDateAddedFormatted() {
        if (dateAdded != null) {
            return dateAdded.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
        return "";
    }

    // Helper method for backward compatibility
    public String getFilename() {
        return microserviceFileId;
    }

    public void setFilename(String filename) {
        this.microserviceFileId = filename;
    }

    // Helper method to check if file is committed
    public boolean isCommitted() {
        return "COMMITTED".equals(status);
    }

    // Helper method to check if file is temporary
    public boolean isTemporary() {
        return "TEMPORARY".equals(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return Objects.equals(id, fileInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", microserviceFileId='" + microserviceFileId + '\'' +
                ", type='" + type + '\'' +
                ", size='" + size + '\'' +
                ", caseNumber='" + caseNumber + '\'' +
                ", dateAdded=" + dateAdded +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", status='" + status + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}