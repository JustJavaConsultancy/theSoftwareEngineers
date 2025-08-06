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
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping
public class GalleryController {

    private final FileFeignClient fileFeignClient;
    private final FileInfoRepository fileInfoRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public GalleryController(FileFeignClient fileFeignClient, FileInfoRepository fileInfoRepository, ObjectMapper objectMapper) {
        this.fileFeignClient = fileFeignClient;
        this.fileInfoRepository = fileInfoRepository;
        this.objectMapper = objectMapper;
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
    public String uploadFile(Model model) {
        // Add case tag options to the model
        List<CaseTagOption> caseTagOptions = getCaseTagOptions();
        model.addAttribute("caseTagOptions", caseTagOptions);
        return "addFile";
    }

    @PostMapping("/addFile")
    public String handleFileUpload(@RequestParam("files") MultipartFile[] files,
                                   @RequestParam(value = "caseTags", required = false) List<String> caseTags,
                                   @RequestParam(value = "caseValues", required = false) List<String> caseValues,
                                   Model model) {

        System.out.println("=== handleFileUpload METHOD CALLED ===");
        System.out.println("Number of files received: " + (files != null ? files.length : "null"));
        System.out.println("Case tags received: " + caseTags);
        System.out.println("Case values received: " + caseValues);

        List<String> uploadResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Build case tags JSON from form data
        String caseTagsJson = buildCaseTagsJson(caseTags, caseValues);
        System.out.println("Built case tags JSON: " + caseTagsJson);

        // Generate case number once for all files in this batch
        long committedCount = fileInfoRepository.countByStatus("COMMITTED");
        String batchCaseNumber = "CASE-" + String.format("%04d", committedCount + 1);
        System.out.println("Generated batch case number: " + batchCaseNumber);

        for (MultipartFile file : files) {
            System.out.println("=== Processing file: " + (file != null ? file.getOriginalFilename() : "null") + " ===");
            System.out.println("File empty check: " + (file != null ? file.isEmpty() : "file is null"));

            if (!file.isEmpty()) {
                try {
                    System.out.println("Calling uploadSingleFile for: " + file.getOriginalFilename());
                    String result = uploadSingleFile(file, caseTagsJson, batchCaseNumber);
                    System.out.println("uploadSingleFile returned: " + result);
                    uploadResults.add("Successfully uploaded: " + file.getOriginalFilename());
                } catch (Exception e) {
                    System.out.println("uploadSingleFile failed for " + file.getOriginalFilename() + ": " + e.getMessage());
                    e.printStackTrace();
                    errors.add("Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            } else {
                System.out.println("Skipping empty file: " + (file != null ? file.getOriginalFilename() : "null"));
            }
        }

        System.out.println("=== Upload processing completed ===");
        System.out.println("Successful uploads: " + uploadResults.size());
        System.out.println("Failed uploads: " + errors.size());

        model.addAttribute("uploadResults", uploadResults);
        model.addAttribute("errors", errors);
        model.addAttribute("caseTagOptions", getCaseTagOptions());

        if (errors.isEmpty()) {
            return "redirect:/gallery";
        } else {
            return "addFile";
        }
    }

    private String uploadSingleFile(MultipartFile file, String caseTagsJson) throws Exception {
        // Generate case number for single file upload
        long committedCount = fileInfoRepository.countByStatus("COMMITTED");
        String caseNumber = "CASE-" + String.format("%04d", committedCount + 1);
        return uploadSingleFile(file, caseTagsJson, caseNumber);
    }

    private String uploadSingleFile(MultipartFile file, String caseTagsJson, String caseNumber) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Create metadata map - only fileName and caseTags
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", originalFilename);
        if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
            metadata.put("caseTags", caseTagsJson);
        }

        System.out.println("=== uploadSingleFile METHOD CALLED ===");
        System.out.println("Processing file: " + originalFilename);
        System.out.println("File extension: " + fileExtension);
        System.out.println("Using case number: " + caseNumber);

        System.out.println("=== DETAILED METADATA ANALYSIS (uploadSingleFile) ===");
        System.out.println("Raw caseTagsJson received: " + caseTagsJson);
        System.out.println("Metadata Map created with " + metadata.size() + " entries:");
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            System.out.println("  Key: '" + entry.getKey() + "' -> Value: '" + entry.getValue() + "'");
            System.out.println("  Value type: " + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null"));
            System.out.println("  Value length: " + (entry.getValue() != null ? entry.getValue().length() : "null"));
        }
        System.out.println("Metadata Map toString(): " + metadata.toString());
        System.out.println("Metadata Map class: " + metadata.getClass().getName());

        // Upload file with metadata to microservice
        String uploadResponse;
        try {
            System.out.println("=== FEIGN CLIENT CALL PREPARATION (uploadSingleFile) ===");
            System.out.println("File details:");
            System.out.println("  - Original filename: " + originalFilename);
            System.out.println("  - File size: " + file.getSize() + " bytes");
            System.out.println("  - Content type: " + file.getContentType());
            System.out.println("Metadata being sent to Feign client:");
            System.out.println("  - Map reference: " + metadata);
            System.out.println("  - Map hashCode: " + metadata.hashCode());

            System.out.println("=== CALLING FEIGN CLIENT uploadWithMetaData (uploadSingleFile) ===");
            System.out.println("About to call: fileFeignClient.uploadWithMetaData(file, metadata)");

            uploadResponse = fileFeignClient.uploadWithMetaData(file, metadata);

            System.out.println("=== FEIGN CLIENT RESPONSE RECEIVED (uploadSingleFile) ===");
            System.out.println("Raw microservice response: '" + uploadResponse + "'");
            System.out.println("Response type: " + (uploadResponse != null ? uploadResponse.getClass().getSimpleName() : "null"));
            System.out.println("Response length: " + (uploadResponse != null ? uploadResponse.length() : "null"));

            if (uploadResponse == null || uploadResponse.trim().isEmpty()) {
                System.out.println("ERROR: Microservice returned null or empty response");
                throw new RuntimeException("Failed to upload file to storage service - no response");
            }
        } catch (Exception e) {
            System.out.println("=== FEIGN CLIENT CALL FAILED (uploadSingleFile) ===");
            System.out.println("Exception type: " + e.getClass().getSimpleName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("413")) {
                throw new RuntimeException("File is too large");
            }
            throw new RuntimeException("Failed to upload file to microservice: " + e.getMessage());
        }

        // Extract ID from response
        String fileId = uploadResponse;
        if (uploadResponse != null && uploadResponse.startsWith("File stored with ID: ")) {
            fileId = uploadResponse.substring("File stored with ID: ".length()).trim();
        }

        // Create file info entity
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(UUID.randomUUID().toString());
        fileInfo.setName(originalFilename);
        fileInfo.setMicroserviceFileId(fileId);
        fileInfo.setType(getFileType(fileExtension));
        fileInfo.setSize(formatFileSize(file.getSize()));
        fileInfo.setCaseNumber(caseNumber);
        fileInfo.setDateAdded(LocalDateTime.now());
        fileInfo.setStatus("COMMITTED");
        fileInfo.setSessionId(null);

        // Store the caseTags JSON for later retrieval
        if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
            fileInfo.setCaseTags(caseTagsJson);
        }

        // Save to database
        fileInfoRepository.save(fileInfo);

        return fileId;
    }

    private String buildCaseTagsJson(List<String> caseTags, List<String> caseValues) {
        if (caseTags == null || caseValues == null) {
            return null;
        }

        List<Map<String, Object>> caseTagsList = new ArrayList<>();
        for (int i = 0; i < Math.min(caseTags.size(), caseValues.size()); i++) {
            if (caseTags.get(i) != null && !caseTags.get(i).trim().isEmpty()) {
                Map<String, Object> caseTag = new HashMap<>();
                caseTag.put("tag", caseTags.get(i));
                caseTag.put("value", caseValues.get(i) != null ? caseValues.get(i) : "");
                caseTag.put("index", i);
                caseTagsList.add(caseTag);
            }
        }

        try {
            return objectMapper.writeValueAsString(caseTagsList);
        } catch (Exception e) {
            return null;
        }
    }

    private List<CaseTagOption> getCaseTagOptions() {
        List<CaseTagOption> options = new ArrayList<>();
        options.add(new CaseTagOption("evidence", "Evidence"));
        options.add(new CaseTagOption("witness-statement", "Witness Statement"));
        options.add(new CaseTagOption("forensic-report", "Forensic Report"));
        options.add(new CaseTagOption("legal-document", "Legal Document"));
        options.add(new CaseTagOption("investigation-notes", "Investigation Notes"));
        options.add(new CaseTagOption("surveillance", "Surveillance"));
        options.add(new CaseTagOption("interview-recording", "Interview Recording"));
        options.add(new CaseTagOption("crime-scene-photo", "Crime Scene Photo"));
        options.add(new CaseTagOption("expert-testimony", "Expert Testimony"));
        options.add(new CaseTagOption("case-summary", "Case Summary"));
        return options;
    }

    @PostMapping("/api/files/uploadWithMetaData")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFileWithMetadata(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caseTags", required = false) String caseTagsJson,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        String sessionId = request.getSession().getId();

        System.out.println("=== uploadWithMetaData endpoint called ===");
        System.out.println("File name: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("File size: " + (file != null ? file.getSize() : "null"));
        System.out.println("Case tags: " + caseTagsJson);

        try {
            if (file == null || file.isEmpty()) {
                System.out.println("ERROR: File is empty or null");
                response.put("status", "error");
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            System.out.println("Processing file: " + originalFilename + " with extension: " + fileExtension);

            // Generate case number for committed files
            long committedCount = fileInfoRepository.countByStatus("COMMITTED");
            String caseNumber = "CASE-" + String.format("%04d", committedCount + 1);

            System.out.println("Generated case number: " + caseNumber);

            // Create metadata map - only fileName and caseTags
            Map<String, String> metadata = new HashMap<>();
            metadata.put("fileName", originalFilename);
            if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                metadata.put("caseTags", caseTagsJson);
            }

            System.out.println("=== DETAILED METADATA ANALYSIS ===");
            System.out.println("Raw caseTagsJson received: " + caseTagsJson);
            System.out.println("Metadata Map created with " + metadata.size() + " entries:");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                System.out.println("  Key: '" + entry.getKey() + "' -> Value: '" + entry.getValue() + "'");
                System.out.println("  Value type: " + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null"));
                System.out.println("  Value length: " + (entry.getValue() != null ? entry.getValue().length() : "null"));
            }
            System.out.println("Metadata Map toString(): " + metadata.toString());
            System.out.println("Metadata Map class: " + metadata.getClass().getName());

            // Upload file with metadata to microservice
            String uploadResponse;
            try {
                System.out.println("=== FEIGN CLIENT CALL PREPARATION ===");
                System.out.println("File details:");
                System.out.println("  - Original filename: " + originalFilename);
                System.out.println("  - File size: " + file.getSize() + " bytes");
                System.out.println("  - Content type: " + file.getContentType());
                System.out.println("Metadata being sent to Feign client:");
                System.out.println("  - Map reference: " + metadata);
                System.out.println("  - Map hashCode: " + metadata.hashCode());

                System.out.println("=== CALLING FEIGN CLIENT uploadWithMetaData ===");
                System.out.println("About to call: fileFeignClient.uploadWithMetaData(file, metadata)");

                uploadResponse = fileFeignClient.uploadWithMetaData(file, metadata);

                System.out.println("=== FEIGN CLIENT RESPONSE RECEIVED ===");
                System.out.println("Raw microservice response: '" + uploadResponse + "'");
                System.out.println("Response type: " + (uploadResponse != null ? uploadResponse.getClass().getSimpleName() : "null"));
                System.out.println("Response length: " + (uploadResponse != null ? uploadResponse.length() : "null"));

                if (uploadResponse == null || uploadResponse.trim().isEmpty()) {
                    System.out.println("ERROR: Microservice returned null or empty response");
                    response.put("status", "error");
                    response.put("message", "Failed to upload file to storage service - no response");
                    return ResponseEntity.status(500).body(response);
                }
            } catch (Exception e) {
                System.out.println("=== Microservice call FAILED ===");
                System.out.println("Exception type: " + e.getClass().getSimpleName());
                System.out.println("Exception message: " + e.getMessage());
                e.printStackTrace();

                String errorMessage = "Failed to upload file to microservice. ";
                if (e.getMessage() != null && e.getMessage().contains("413")) {
                    errorMessage = "File is too large";
                } else if (e.getMessage() != null) {
                    errorMessage += "Error: " + e.getMessage();
                }
                response.put("status", "error");
                response.put("message", errorMessage);
                return ResponseEntity.status(500).body(response);
            }

            // Extract ID from response like "File stored with ID: abc123"
            String fileId = uploadResponse;
            if (uploadResponse != null && uploadResponse.startsWith("File stored with ID: ")) {
                fileId = uploadResponse.substring("File stored with ID: ".length()).trim();
            }
            System.out.println("Extracted file ID: " + fileId);

            // Create file info entity
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(UUID.randomUUID().toString());
            fileInfo.setName(originalFilename);
            fileInfo.setMicroserviceFileId(fileId);
            fileInfo.setType(getFileType(fileExtension));
            fileInfo.setSize(formatFileSize(file.getSize()));
            fileInfo.setCaseNumber(caseNumber);
            fileInfo.setDateAdded(LocalDateTime.now());
            fileInfo.setStatus("COMMITTED"); // All files are committed immediately
            fileInfo.setSessionId(null); // No session tracking needed

            // Store the caseTags JSON for later retrieval
            if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                fileInfo.setCaseTags(caseTagsJson);
            }

            System.out.println("=== Saving to database ===");
            // Save to database
            try {
                fileInfo = fileInfoRepository.save(fileInfo);
                System.out.println("File info saved with ID: " + fileInfo.getId());
            } catch (Exception e) {
                System.out.println("ERROR: Failed to save to database");
                e.printStackTrace();
                response.put("status", "error");
                response.put("message", "Failed to save file info to database: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }

            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("fileInfo", convertToDTO(fileInfo));

            System.out.println("=== Upload completed successfully ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("=== UNEXPECTED ERROR in uploadWithMetaData ===");
            System.out.println("Exception type: " + e.getClass().getSimpleName());
            System.out.println("Exception message: " + e.getMessage());
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
            @RequestParam(value = "caseTags", required = false) String caseTagsJson,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        String sessionId = request.getSession().getId();

        try {
            List<FileInfo> uploadedFiles = new ArrayList<>();

            // Generate case number once for all files in this batch
            long committedCount = fileInfoRepository.countByStatus("COMMITTED");
            String batchCaseNumber = "CASE-" + String.format("%04d", committedCount + 1);

            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                // Use the same case number for all files in this batch
                String caseNumber = batchCaseNumber;

                // Create metadata map - only fileName and caseTags
                Map<String, String> metadata = new HashMap<>();
                metadata.put("fileName", originalFilename);
                if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                    metadata.put("caseTags", caseTagsJson);
                }

                // Upload to microservice with metadata
                String uploadResponse;
                try {
                    uploadResponse = fileFeignClient.uploadWithMetaData(file, metadata);
                    if (uploadResponse == null) {
                        throw new RuntimeException("Microservice upload failed for: " + originalFilename);
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("413")) {
                        throw new RuntimeException("File '" + originalFilename + "' is too large for the storage service (max 10MB recommended)");
                    }
                    throw new RuntimeException("Microservice upload failed for: " + originalFilename + " - " + e.getMessage());
                }

                // Extract file ID from response
                String fileId = uploadResponse;
                if (uploadResponse != null && uploadResponse.startsWith("File stored with ID: ")) {
                    fileId = uploadResponse.substring("File stored with ID: ".length()).trim();
                }

                FileInfo fileInfo = new FileInfo();
                fileInfo.setId(UUID.randomUUID().toString());
                fileInfo.setName(originalFilename);
                fileInfo.setMicroserviceFileId(fileId);
                fileInfo.setType(getFileType(fileExtension));
                fileInfo.setSize(formatFileSize(file.getSize()));
                fileInfo.setDateAdded(LocalDateTime.now());
                fileInfo.setStatus("COMMITTED"); // Multiple upload commits immediately
                fileInfo.setCaseNumber(caseNumber);

                // Store the caseTags JSON for later retrieval
                if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                    fileInfo.setCaseTags(caseTagsJson);
                }

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
            String actualType = "all".equalsIgnoreCase(type) ? null : type;
            files = fileInfoRepository.searchCommittedFiles(query, actualType);
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
            Optional<FileInfo> fileOptional = fileInfoRepository.findById(fileId);

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
        dto.setCaseTags(fileInfo.getCaseTags());
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
        private String caseTags;

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

        public String getCaseTags() { return caseTags; }
        public void setCaseTags(String caseTags) { this.caseTags = caseTags; }
    }

    @GetMapping("/case-tags")
    public String caseTagsSummary(Model model) {
        List<FileInfo> allFiles = fileInfoRepository.findByStatusOrderByDateAddedDesc("COMMITTED");

        // Group files by case numbers
        Map<String, CaseTagSummary> caseNumberGroups = new HashMap<>();

        for (FileInfo file : allFiles) {
            String caseNumber = file.getCaseNumber();

            if (caseNumber != null && !caseNumber.trim().isEmpty()) {
                if (!caseNumberGroups.containsKey(caseNumber)) {
                    CaseTagSummary summary = new CaseTagSummary();
                    summary.setTag(caseNumber); // Use case number as tag for URL
                    summary.setLabel(caseNumber); // Display case number as label
                    summary.setFiles(new ArrayList<>());
                    caseNumberGroups.put(caseNumber, summary);
                }
                caseNumberGroups.get(caseNumber).getFiles().add(convertToDTO(file));
            }
        }

        // Generate summaries for each case
        for (CaseTagSummary summary : caseNumberGroups.values()) {
            String generatedSummary = generateCaseSummary(summary);
            summary.setSummary(generatedSummary);

            List<String> bulletPoints = generateCaseBulletPoints(summary);
            summary.setBulletPoints(bulletPoints);
        }

        // Convert to list and sort by case number
        List<CaseTagSummary> caseTagSummaries = new ArrayList<>(caseNumberGroups.values());
        caseTagSummaries.sort((a, b) -> a.getLabel().compareTo(b.getLabel()));

        model.addAttribute("caseTagSummaries", caseTagSummaries);
        return "caseTagSummary";
    }

    private String generateCaseSummary(CaseTagSummary caseTagSummary) {
        List<FileInfoDTO> files = caseTagSummary.getFiles();
        int fileCount = files.size();
        String caseNumber = caseTagSummary.getLabel();

        // Analyze file types
        Map<String, Integer> fileTypeCount = new HashMap<>();
        Set<String> caseTagTypes = new HashSet<>();

        for (FileInfoDTO file : files) {
            // Count file types
            String fileType = file.getType();
            fileTypeCount.put(fileType, fileTypeCount.getOrDefault(fileType, 0) + 1);

            // Extract case tag types from the file
            try {
                String caseTagsJson = file.getCaseTags();
                if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                    List<Map<String, Object>> caseTagsList = objectMapper.readValue(caseTagsJson, List.class);
                    for (Map<String, Object> caseTagMap : caseTagsList) {
                        String tagType = (String) caseTagMap.get("tag");
                        if (tagType != null) {
                            caseTagTypes.add(tagType);
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing fails, continue without case tag analysis
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Case ").append(caseNumber).append(" contains ")
                .append(fileCount).append(fileCount == 1 ? " file" : " files");

        // Add file type breakdown
        if (!fileTypeCount.isEmpty()) {
            summary.append(" including ");
            List<String> typeDescriptions = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : fileTypeCount.entrySet()) {
                int count = entry.getValue();
                String type = entry.getKey();
                typeDescriptions.add(count + " " + type + (count == 1 ? "" : ""));
            }

            summary.append(String.join(", ", typeDescriptions));
        }

        summary.append(".");

        // Add case tag information
        if (!caseTagTypes.isEmpty()) {
            summary.append(" This case includes ");
            List<String> tagLabels = new ArrayList<>();

            for (String tagType : caseTagTypes) {
                String tagLabel = getTagLabelFromType(tagType);
                tagLabels.add(tagLabel.toLowerCase());
            }

            if (tagLabels.size() == 1) {
                summary.append(tagLabels.get(0));
            } else if (tagLabels.size() == 2) {
                summary.append(tagLabels.get(0)).append(" and ").append(tagLabels.get(1));
            } else {
                summary.append(String.join(", ", tagLabels.subList(0, tagLabels.size() - 1)))
                        .append(", and ").append(tagLabels.get(tagLabels.size() - 1));
            }
            summary.append(".");
        }

        return summary.toString();
    }

    private String getTagLabelFromType(String tagType) {
        List<CaseTagOption> options = getCaseTagOptions();
        for (CaseTagOption option : options) {
            if (option.getValue().equals(tagType)) {
                return option.getLabel();
            }
        }
        return tagType; // fallback to the tag type itself
    }

    private List<String> generateCaseBulletPoints(CaseTagSummary caseTagSummary) {
        List<String> bulletPoints = new ArrayList<>();
        List<FileInfoDTO> files = caseTagSummary.getFiles();
        String caseNumber = caseTagSummary.getLabel();

        // Analyze case data to generate relevant bullet points
        Set<String> caseTagTypes = new HashSet<>();
        Map<String, Integer> fileTypeCount = new HashMap<>();

        for (FileInfoDTO file : files) {
            // Count file types
            String fileType = file.getType();
            fileTypeCount.put(fileType, fileTypeCount.getOrDefault(fileType, 0) + 1);

            // Extract case tag types
            try {
                String caseTagsJson = file.getCaseTags();
                if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                    List<Map<String, Object>> caseTagsList = objectMapper.readValue(caseTagsJson, List.class);
                    for (Map<String, Object> caseTagMap : caseTagsList) {
                        String tagType = (String) caseTagMap.get("tag");
                        if (tagType != null) {
                            caseTagTypes.add(tagType);
                        }
                    }
                }
            } catch (Exception e) {
                // Continue without case tag analysis if parsing fails
            }
        }

        // Generate dynamic bullet points based on case content
        bulletPoints.add("Contains " + files.size() + " file" + (files.size() == 1 ? "" : "s") + " related to " + caseNumber + " investigation");

        // Add file type specific bullet points
        if (fileTypeCount.containsKey("images") && fileTypeCount.get("images") > 0) {
            bulletPoints.add("Includes " + fileTypeCount.get("images") + " image file" + (fileTypeCount.get("images") == 1 ? "" : "s") + " for visual evidence");
        }

        if (fileTypeCount.containsKey("documents") && fileTypeCount.get("documents") > 0) {
            bulletPoints.add("Contains " + fileTypeCount.get("documents") + " document" + (fileTypeCount.get("documents") == 1 ? "" : "s") + " with case documentation");
        }

        if (fileTypeCount.containsKey("videos") && fileTypeCount.get("videos") > 0) {
            bulletPoints.add("Features " + fileTypeCount.get("videos") + " video file" + (fileTypeCount.get("videos") == 1 ? "" : "s") + " for multimedia evidence");
        }

        if (fileTypeCount.containsKey("audio") && fileTypeCount.get("audio") > 0) {
            bulletPoints.add("Includes " + fileTypeCount.get("audio") + " audio file" + (fileTypeCount.get("audio") == 1 ? "" : "s") + " for recorded evidence");
        }

        // Add case tag specific bullet points
        if (caseTagTypes.contains("evidence")) {
            bulletPoints.add("Organized with evidence tags for legal proceedings");
        }

        if (caseTagTypes.contains("witness-statement")) {
            bulletPoints.add("Contains witness statements for case testimony");
        }

        if (caseTagTypes.contains("forensic-report")) {
            bulletPoints.add("Includes forensic analysis reports");
        }

        if (caseTagTypes.contains("crime-scene-photo")) {
            bulletPoints.add("Features crime scene photography documentation");
        }

        // Add general organizational bullet point
        if (!caseTagTypes.isEmpty()) {
            bulletPoints.add("Categorized by case tags for efficient case management");
        } else {
            bulletPoints.add("Ready for case tag categorization and organization");
        }

        return bulletPoints;
    }

    @GetMapping("/case-tags/{caseNumber}")
    public String caseTagDetail(@PathVariable String caseNumber, Model model) {
        List<FileInfo> allFiles = fileInfoRepository.findByStatusAndCaseNumberOrderByDateAddedDesc("COMMITTED", caseNumber);

        // Group files by case tag types
        Map<String, List<FileInfoDTO>> caseTagGroups = new HashMap<>();
        List<CaseTagOption> caseTagOptions = getCaseTagOptions();

        // Initialize groups for all case tag options
        for (CaseTagOption option : caseTagOptions) {
            caseTagGroups.put(option.getValue(), new ArrayList<>());
        }

        int totalFiles = 0;
        for (FileInfo file : allFiles) {
            List<CaseTagInfo> caseTags = parseCaseTagsFromFile(file);
            FileInfoDTO fileDTO = convertToDTO(file);
            totalFiles++;

            for (CaseTagInfo caseTag : caseTags) {
                String tagType = caseTag.getTag();
                if (caseTagGroups.containsKey(tagType)) {
                    caseTagGroups.get(tagType).add(fileDTO);
                }
            }
        }

        // Create grouped case tag data for the view
        List<CaseTagGroup> caseTagGroupList = new ArrayList<>();
        for (CaseTagOption option : caseTagOptions) {
            List<FileInfoDTO> files = caseTagGroups.get(option.getValue());
            if (!files.isEmpty()) {
                CaseTagGroup group = new CaseTagGroup();
                group.setTagType(option.getValue());
                group.setTagLabel(option.getLabel());
                group.setFiles(files);
                caseTagGroupList.add(group);
            }
        }

        model.addAttribute("caseNumber", caseNumber);
        model.addAttribute("caseTagGroups", caseTagGroupList);
        model.addAttribute("totalFiles", totalFiles);

        return "caseTagDetails";
    }

    private List<CaseTagInfo> parseCaseTagsFromFile(FileInfo file) {
        List<CaseTagInfo> caseTags = new ArrayList<>();

        try {
            String caseTagsJson = file.getCaseTags();

            if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                // Parse the actual caseTags JSON that was stored during upload
                List<Map<String, Object>> caseTagsList = objectMapper.readValue(caseTagsJson, List.class);

                for (Map<String, Object> caseTagMap : caseTagsList) {
                    CaseTagInfo caseTag = new CaseTagInfo();
                    caseTag.setTag((String) caseTagMap.get("tag"));
                    caseTag.setValue((String) caseTagMap.get("value"));
                    caseTag.setIndex((Integer) caseTagMap.getOrDefault("index", 0));
                    caseTags.add(caseTag);
                }
            } else {
                // Fallback: if no caseTags stored, create a default one
                CaseTagInfo defaultCaseTag = new CaseTagInfo();
                defaultCaseTag.setTag("evidence");
                defaultCaseTag.setValue("General evidence for " + file.getCaseNumber());
                defaultCaseTag.setIndex(0);
                caseTags.add(defaultCaseTag);
            }

        } catch (Exception e) {
            System.out.println("Error parsing case tags for file " + file.getId() + ": " + e.getMessage());
            e.printStackTrace();

            // Fallback: create a default case tag if parsing fails
            CaseTagInfo defaultCaseTag = new CaseTagInfo();
            defaultCaseTag.setTag("evidence");
            defaultCaseTag.setValue("General evidence for " + file.getCaseNumber());
            defaultCaseTag.setIndex(0);
            caseTags.add(defaultCaseTag);
        }

        return caseTags;
    }

    // Case tag option class for the view
    public static class CaseTagOption {
        private String value;
        private String label;

        public CaseTagOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    // Case tag summary class for the summary view
    public static class CaseTagSummary {
        private String tag;
        private String label;
        private List<FileInfoDTO> files;
        private String summary;
        private List<String> bulletPoints;

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public List<FileInfoDTO> getFiles() { return files; }
        public void setFiles(List<FileInfoDTO> files) { this.files = files; }

        public int getFileCount() { return files != null ? files.size() : 0; }

        public String getDisplayName() { return label; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public List<String> getBulletPoints() { return bulletPoints; }
        public void setBulletPoints(List<String> bulletPoints) { this.bulletPoints = bulletPoints; }
    }

    // Case tag detail info class for the detail view
    public static class CaseTagDetailInfo {
        private FileInfoDTO file;
        private String caseTagValue;
        private int caseTagIndex;

        public FileInfoDTO getFile() { return file; }
        public void setFile(FileInfoDTO file) { this.file = file; }

        public String getCaseTagValue() { return caseTagValue; }
        public void setCaseTagValue(String caseTagValue) { this.caseTagValue = caseTagValue; }

        public int getCaseTagIndex() { return caseTagIndex; }
        public void setCaseTagIndex(int caseTagIndex) { this.caseTagIndex = caseTagIndex; }
    }

    // Case tag info class for parsing
    public static class CaseTagInfo {
        private String tag;
        private String value;
        private int index;

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
    }

    // Case tag group class for the details view
    public static class CaseTagGroup {
        private String tagType;
        private String tagLabel;
        private List<FileInfoDTO> files;

        public String getTagType() { return tagType; }
        public void setTagType(String tagType) { this.tagType = tagType; }

        public String getTagLabel() { return tagLabel; }
        public void setTagLabel(String tagLabel) { this.tagLabel = tagLabel; }

        public List<FileInfoDTO> getFiles() { return files; }
        public void setFiles(List<FileInfoDTO> files) { this.files = files; }

        public int getFileCount() { return files != null ? files.size() : 0; }
    }
}
