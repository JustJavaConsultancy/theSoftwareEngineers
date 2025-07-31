package tech.justjava.process_manager.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping
public class GalleryController {

    private final FileFeignClient fileFeignClient;
    private final FileInfoRepository fileInfoRepository;

    @Autowired
    public GalleryController(FileFeignClient fileFeignClient, FileInfoRepository fileInfoRepository) {
        this.fileFeignClient = fileFeignClient;
        this.fileInfoRepository = fileInfoRepository;
    }

    @GetMapping("/gallery")
    public String gallery(Model model,
                          @RequestParam(value = "type", defaultValue = "all") String type,
                          HttpServletRequest request) {

        List<FileInfo> files;

        if ("all".equals(type)) {
            files = fileInfoRepository.findByStatusOrderByDateAddedDesc("COMMITTED");
        } else {
            files = fileInfoRepository.findByStatusAndTypeOrderByDateAddedDesc("COMMITTED", type);
        }

        // Convert LocalDateTime to formatted string for the view
        List<FileInfoDTO> fileDTOs = files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        model.addAttribute("files", fileDTOs);
        model.addAttribute("activeTab", type);
        return "gallery";
    }

    @GetMapping("/addFile")
    public String uploadFile() {
        return "addFile";
    }

    @PostMapping("/api/files/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String sessionId = request.getSession().getId();

        try {
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload file to microservice
            ResponseEntity<String> uploadResponse;
            try {
                uploadResponse = fileFeignClient.uploadFile(file);
                if (!uploadResponse.getStatusCode().is2xxSuccessful() || uploadResponse.getBody() == null) {
                    String errorMessage = "Failed to upload file. ";
                    if (uploadResponse.getStatusCode().value() == 413) {
                        errorMessage = "File is too large";
                    }
                    response.put("status", "error");
                    response.put("message", errorMessage);
                    return ResponseEntity.status(uploadResponse.getStatusCode().value()).body(response);
                }
            } catch (Exception e) {
                String errorMessage = "Failed to upload file. ";
                if (e.getMessage() != null && e.getMessage().contains("413")) {
                    errorMessage = "File is too large";
                }
                response.put("status", "error");
                response.put("message", errorMessage);
                return ResponseEntity.status(500).body(response);
            }

            String responseBody = uploadResponse.getBody();
            System.out.println("Upload response: " + responseBody);

            // Extract ID from response like "File stored with ID: abc123"
            String fileId = responseBody;
            if (responseBody != null && responseBody.startsWith("File stored with ID: ")) {
                fileId = responseBody.substring("File stored with ID: ".length()).trim();
            }
            System.out.println("Extracted file ID: " + fileId);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Create file info entity
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(UUID.randomUUID().toString());
            fileInfo.setName(originalFilename);
            fileInfo.setMicroserviceFileId(fileId);
            fileInfo.setType(getFileType(fileExtension));
            fileInfo.setSize(formatFileSize(file.getSize()));
            fileInfo.setCaseNumber(""); // Will be set when committed
            fileInfo.setDateAdded(LocalDateTime.now());
            fileInfo.setStatus("TEMPORARY");
            fileInfo.setSessionId(sessionId);

            // Save to database as temporary file
            fileInfo = fileInfoRepository.save(fileInfo);

            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("fileInfo", convertToDTO(fileInfo));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/api/files/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        String sessionId = request.getSession().getId();

        try {
            List<FileInfo> uploadedFiles = new ArrayList<>();

            for (MultipartFile file : files) {
                // 1. Upload to microservice
                ResponseEntity<String> uploadResponse;
                try {
                    uploadResponse = fileFeignClient.uploadFile(file);
                    if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                        if (uploadResponse.getStatusCode().value() == 413) {
                            throw new RuntimeException("File '" + file.getOriginalFilename() + "' is too large for the storage service (max 10MB recommended)");
                        }
                        throw new RuntimeException("Microservice upload failed for: " + file.getOriginalFilename());
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("413")) {
                        throw new RuntimeException("File '" + file.getOriginalFilename() + "' is too large for the storage service (max 10MB recommended)");
                    }
                    throw new RuntimeException("Microservice upload failed for: " + file.getOriginalFilename() + " - " + e.getMessage());
                }

                // 2. Create and store file info
                String responseBody = uploadResponse.getBody();
                String fileId = responseBody;
                if (responseBody != null && responseBody.startsWith("File stored with ID: ")) {
                    fileId = responseBody.substring("File stored with ID: ".length()).trim();
                }

                FileInfo fileInfo = new FileInfo();
                fileInfo.setId(UUID.randomUUID().toString());
                fileInfo.setName(file.getOriginalFilename());
                fileInfo.setMicroserviceFileId(fileId);
                fileInfo.setType(getFileType(file.getContentType()));
                fileInfo.setSize(formatFileSize(file.getSize()));
                fileInfo.setDateAdded(LocalDateTime.now());
                fileInfo.setStatus("COMMITTED"); // Multiple upload commits immediately

                // Generate case number for committed files
                long committedCount = fileInfoRepository.countByStatus("COMMITTED");
                fileInfo.setCaseNumber("CASE-" + String.format("%04d", committedCount + uploadedFiles.size() + 1));

                fileInfo = fileInfoRepository.save(fileInfo);
                uploadedFiles.add(fileInfo);
            }

            response.put("status", "success");
            response.put("message", files.length + " files uploaded successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private FileInfo createFileInfo(MultipartFile file, String microserviceId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(UUID.randomUUID().toString());
        fileInfo.setName(file.getOriginalFilename());
        fileInfo.setMicroserviceFileId(microserviceId);

        // Set other properties (type, size, etc.)
        String ext = "";
        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
            ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        }
        fileInfo.setType(getFileType(ext));
        fileInfo.setSize(formatFileSize(file.getSize()));
        fileInfo.setDateAdded(LocalDateTime.now());

        return fileInfo;
    }

    @GetMapping("/api/files/search")
    @ResponseBody
    public ResponseEntity<List<FileInfoDTO>> searchFiles(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "type", defaultValue = "all") String type,
            HttpServletRequest request) {

        List<FileInfo> files;

        if (query.isEmpty()) {
            if ("all".equals(type)) {
                files = fileInfoRepository.findByStatusOrderByDateAddedDesc("COMMITTED");
            } else {
                files = fileInfoRepository.findByStatusAndTypeOrderByDateAddedDesc("COMMITTED", type);
            }
        } else {
            if ("all".equals(type)) {
                files = fileInfoRepository.searchCommittedFiles(query);
            } else {
                files = fileInfoRepository.searchCommittedFilesByType(query, type);
            }
        }

        List<FileInfoDTO> fileDTOs = files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileDTOs);
    }

    @DeleteMapping("/api/files/{fileId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileId,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<FileInfo> fileOptional = fileInfoRepository.findByMicroserviceFileId(fileId);

            if (!fileOptional.isPresent()) {
                response.put("status", "error");
                response.put("message", "File not found");
                return ResponseEntity.notFound().build();
            }

            FileInfo file = fileOptional.get();

            // Delete file from microservice
            ResponseEntity<String> deleteResponse = fileFeignClient.deleteFile(file.getMicroserviceFileId());
            System.out.println("Delete ID: " + fileId);
            if (!deleteResponse.getStatusCode().is2xxSuccessful()) {
                response.put("status", "error");
                response.put("message", "Failed to delete file from storage service");
                return ResponseEntity.status(500).body(response);
            }

            // Remove from database
            fileInfoRepository.delete(file);

            response.put("status", "success");
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/files/download/{fileId}")
    @ResponseBody
    public ResponseEntity<?> downloadFile(@PathVariable String fileId, HttpServletRequest request) {
        try {
            // Find the file in database to get the microservice file ID
            Optional<FileInfo> fileOptional = fileInfoRepository.findById(fileId);

            if (!fileOptional.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            FileInfo fileInfo = fileOptional.get();
            String microserviceFileId = fileInfo.getMicroserviceFileId();

            System.out.println("Downloading file - Local ID: " + fileId + ", Microservice ID: " + microserviceFileId);
            return fileFeignClient.downloadFile(microserviceFileId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to download file: " + e.getMessage());
        }
    }

    @PostMapping("/api/files/commit-uploads")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> commitUploads(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String sessionId = request.getSession().getId();

        try {
            // Find temporary files for this session
            List<FileInfo> tempFiles = fileInfoRepository.findByStatusAndSessionIdOrderByDateAddedDesc("TEMPORARY", sessionId);

            if (tempFiles.isEmpty()) {
                response.put("status", "success");
                response.put("message", "No files to commit");
                response.put("filesCommitted", 0);
                return ResponseEntity.ok(response);
            }

            // Get current count of committed files for case number generation
            long currentCount = fileInfoRepository.countByStatus("COMMITTED");

            // Update temporary files to committed status and assign case numbers
            for (int i = 0; i < tempFiles.size(); i++) {
                FileInfo tempFile = tempFiles.get(i);
                tempFile.setStatus("COMMITTED");
                tempFile.setCaseNumber("CASE-" + String.format("%04d", currentCount + i + 1));
                tempFile.setSessionId(null); // Clear session ID for committed files
                fileInfoRepository.save(tempFile);
            }

            response.put("status", "success");
            response.put("message", tempFiles.size() + " files committed to gallery");
            response.put("filesCommitted", tempFiles.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to commit uploads: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String getFileType(String fileExtension) {
        if (fileExtension == null) return "documents";

        String ext = fileExtension.toLowerCase();
        if (ext.matches("\\.(jpg|jpeg|png|gif|bmp|svg)")) {
            return "images";
        } else if (ext.matches("\\.(mp4|avi|mov|wmv|flv|webm)")) {
            return "videos";
        } else if (ext.matches("\\.(mp3|wav|flac|aac|ogg)")) {
            return "audio";
        } else {
            return "documents";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // Helper method to convert FileInfo entity to DTO for the view
    private FileInfoDTO convertToDTO(FileInfo fileInfo) {
        FileInfoDTO dto = new FileInfoDTO();
        dto.setId(fileInfo.getId());
        dto.setName(fileInfo.getName());
        dto.setMicroserviceFileId(fileInfo.getMicroserviceFileId());
        dto.setType(fileInfo.getType());
        dto.setSize(fileInfo.getSize());
        dto.setCaseNumber(fileInfo.getCaseNumber());
        dto.setDateAdded(fileInfo.getDateAddedFormatted());
        return dto;
    }

    // DTO class for view compatibility
    public static class FileInfoDTO {
        private String id;
        private String name;
        private String microserviceFileId;
        private String type;
        private String size;
        private String caseNumber;
        private String dateAdded;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getMicroserviceFileId() { return microserviceFileId; }
        public void setMicroserviceFileId(String microserviceFileId) { this.microserviceFileId = microserviceFileId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

        public String getDateAdded() { return dateAdded; }
        public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

        public String getFilename() { return microserviceFileId; }
        public void setFilename(String filename) { this.microserviceFileId = filename; }
    }
}
