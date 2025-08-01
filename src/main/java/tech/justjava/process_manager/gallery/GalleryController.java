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

        List<String> uploadResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Build case tags JSON from form data
        String caseTagsJson = buildCaseTagsJson(caseTags, caseValues);

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String result = uploadSingleFile(file, caseTagsJson);
                    uploadResults.add("Successfully uploaded: " + file.getOriginalFilename());
                } catch (Exception e) {
                    errors.add("Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }
        }

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
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate case number for committed files
        long committedCount = fileInfoRepository.countByStatus("COMMITTED");
        String caseNumber = "CASE-" + String.format("%04d", committedCount + 1);

        // Create metadata map - only fileName and caseTags
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", originalFilename);
        if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
            metadata.put("caseTags", caseTagsJson);
        }

        // Upload file with metadata to microservice
        String uploadResponse;
        try {
            // Convert metadata map to JSON string
            String metadataJson = objectMapper.writeValueAsString(metadata);
            uploadResponse = fileFeignClient.uploadWithMetaData(file, metadataJson);

            if (uploadResponse == null || uploadResponse.trim().isEmpty()) {
                throw new RuntimeException("Failed to upload file to storage service - no response");
            }
        } catch (Exception e) {
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

            System.out.println("=== Metadata created ===");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

            // Upload file with metadata to microservice
            String uploadResponse;
            try {
                System.out.println("=== Calling microservice uploadWithMetaData ===");
                // Convert metadata map to JSON string
                String metadataJson = objectMapper.writeValueAsString(metadata);
                System.out.println("Metadata JSON: " + metadataJson);

                uploadResponse = fileFeignClient.uploadWithMetaData(file, metadataJson);
                System.out.println("Microservice upload response: " + uploadResponse);

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

            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                // Generate case number for committed files
                long committedCount = fileInfoRepository.countByStatus("COMMITTED");
                String caseNumber = "CASE-" + String.format("%04d", committedCount + uploadedFiles.size() + 1);

                // Create metadata map - only fileName and caseTags
                Map<String, String> metadata = new HashMap<>();
                metadata.put("fileName", originalFilename);
                if (caseTagsJson != null && !caseTagsJson.trim().isEmpty()) {
                    metadata.put("caseTags", caseTagsJson);
                }

                // Upload to microservice with metadata
                String uploadResponse;
                try {
                    // Convert metadata map to JSON string
                    String metadataJson = objectMapper.writeValueAsString(metadata);
                    uploadResponse = fileFeignClient.uploadWithMetaData(file, metadataJson);
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
}
