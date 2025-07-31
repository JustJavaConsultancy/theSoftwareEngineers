package tech.justjava.process_manager.gallery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, String> {

    // Find all committed files (visible in gallery)
    List<FileInfo> findByStatusOrderByDateAddedDesc(String status);

    // Find committed files by type
    List<FileInfo> findByStatusAndTypeOrderByDateAddedDesc(String status, String type);

    // Find temporary files by session ID
    List<FileInfo> findByStatusAndSessionIdOrderByDateAddedDesc(String status, String sessionId);

    // Search committed files by name or case number
    @Query("SELECT f FROM FileInfo f WHERE f.status = 'COMMITTED' AND " +
            "(LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.caseNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<FileInfo> searchCommittedFiles(@Param("query") String query);

    // Search committed files by name or case number and type
    @Query("SELECT f FROM FileInfo f WHERE f.status = 'COMMITTED' AND f.type = :type AND " +
            "(LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.caseNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<FileInfo> searchCommittedFilesByType(@Param("query") String query, @Param("type") String type);

    // Find by microservice file ID (useful for downloads and deletes)
    Optional<FileInfo> findByMicroserviceFileId(String microserviceFileId);

    // Count committed files (for case number generation)
    long countByStatus(String status);

    // Delete temporary files by session ID (cleanup)
    void deleteByStatusAndSessionId(String status, String sessionId);

    // Find all temporary files older than a certain time (for cleanup)
    @Query("SELECT f FROM FileInfo f WHERE f.status = 'TEMPORARY' AND f.dateAdded < :cutoffTime")
    List<FileInfo> findOldTemporaryFiles(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
}