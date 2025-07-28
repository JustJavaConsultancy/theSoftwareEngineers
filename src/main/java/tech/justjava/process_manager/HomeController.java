package tech.justjava.process_manager;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.justjava.process_manager.support.SupportFeignClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final SupportFeignClient supportFeignClient;

    @Value("${app.processKey}")
    private String processKey;

    private final String uploadDir = "uploads/";
    private final String tempUploadDir = "temp-uploads/";
    private final List<FileInfo> uploadedFiles = new ArrayList<>();
    private final Map<String, List<FileInfo>> sessionFiles = new HashMap<>();

    public HomeController(
            RuntimeService runtimeService,
            TaskService taskService,
            HistoryService historyService,
            SupportFeignClient supportFeignClient
    ) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.supportFeignClient = supportFeignClient;

        // Create upload directories if they don't exist
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(tempUploadDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/gallery")
    public String gallery(Model model, @RequestParam(value = "type", defaultValue = "all") String type,
                          HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        List<FileInfo> sessionFileList = sessionFiles.getOrDefault(sessionId, new ArrayList<>());

        // Combine session files with permanent files
        List<FileInfo> allFiles = new ArrayList<>(uploadedFiles);
        allFiles.addAll(sessionFileList);

        List<FileInfo> filteredFiles;
        if ("all".equals(type)) {
            filteredFiles = allFiles;
        } else {
            filteredFiles = allFiles.stream()
                    .filter(file -> type.equalsIgnoreCase(file.getType()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("files", filteredFiles);
        model.addAttribute("activeTab", type);
        return "gallery";
    }

    @GetMapping("/addFile")
    public String uploadFile() {
        return "addFile";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long processInstancesCount = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processKey)
                .active()
                .count();

        List<Task> activeTasks = taskService.createTaskQuery()
                .processDefinitionKey(processKey)
                .active()
                .list();

        long activeTasksCount = activeTasks.size();

        long completedTasksCount = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processKey)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .count();

        model.addAttribute("processInstancesCount", processInstancesCount);
        model.addAttribute("activeTasksCount", activeTasksCount);
        model.addAttribute("completedTasksCount", completedTasksCount);
        model.addAttribute("activeTasks", activeTasks);

        return "dashboard";
    }

    @PostMapping("/api/files/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;

            // Save file to temporary directory
            Path filePath = Paths.get(tempUploadDir + uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create file info but don't add to permanent storage yet
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(UUID.randomUUID().toString());
            fileInfo.setName(originalFilename);
            fileInfo.setFilename(uniqueFilename);
            fileInfo.setType(getFileType(fileExtension));
            fileInfo.setSize(formatFileSize(file.getSize()));
            fileInfo.setCaseNumber("CASE-" + String.format("%04d", uploadedFiles.size() + 1));
            fileInfo.setDateAdded(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            fileInfo.setTemporary(true);

            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("fileInfo", fileInfo);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/api/files/{fileId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileId,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String sessionId = request.getSession().getId();
            List<FileInfo> sessionFileList = sessionFiles.getOrDefault(sessionId, new ArrayList<>());

            // Try to find in session files first
            Optional<FileInfo> sessionFile = sessionFileList.stream()
                    .filter(file -> file.getId().equals(fileId))
                    .findFirst();

            if (sessionFile.isPresent()) {
                FileInfo file = sessionFile.get();

                // Delete physical file
                Path filePath = Paths.get(uploadDir + file.getFilename());
                Files.deleteIfExists(filePath);

                // Remove from session list
                sessionFileList.remove(file);

                response.put("status", "success");
                response.put("message", "File deleted successfully");
                return ResponseEntity.ok(response);
            }

            // Try to find in permanent files
            Optional<FileInfo> permanentFile = uploadedFiles.stream()
                    .filter(file -> file.getId().equals(fileId))
                    .findFirst();

            if (permanentFile.isPresent()) {
                FileInfo file = permanentFile.get();

                // Delete physical file
                Path filePath = Paths.get(uploadDir + file.getFilename());
                Files.deleteIfExists(filePath);

                // Remove from permanent list
                uploadedFiles.remove(file);

                response.put("status", "success");
                response.put("message", "File deleted successfully");
                return ResponseEntity.ok(response);
            }

            response.put("status", "error");
            response.put("message", "File not found");
            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/api/files/finalize")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizeUploads(@RequestBody List<FileInfo> tempFiles,
                                                               HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String sessionId = request.getSession().getId();
            List<FileInfo> sessionFileList = sessionFiles.computeIfAbsent(sessionId, k -> new ArrayList<>());

            for (FileInfo tempFile : tempFiles) {
                // Move file from temp to permanent directory
                Path tempPath = Paths.get(tempUploadDir + tempFile.getFilename());
                Path permanentPath = Paths.get(uploadDir + tempFile.getFilename());

                if (Files.exists(tempPath)) {
                    Files.move(tempPath, permanentPath, StandardCopyOption.REPLACE_EXISTING);
                    tempFile.setTemporary(false);

                    // Add to session storage (will persist until session ends)
                    sessionFileList.add(tempFile);
                }
            }

            response.put("status", "success");
            response.put("message", "Files finalized successfully");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to finalize files: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/files/search")
    @ResponseBody
    public ResponseEntity<List<FileInfo>> searchFiles(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "type", defaultValue = "all") String type,
            HttpServletRequest request) {

        String sessionId = request.getSession().getId();
        List<FileInfo> sessionFileList = sessionFiles.getOrDefault(sessionId, new ArrayList<>());

        // Combine session files with permanent files
        List<FileInfo> allFiles = new ArrayList<>(uploadedFiles);
        allFiles.addAll(sessionFileList);

        List<FileInfo> filteredFiles = allFiles.stream()
                .filter(file -> {
                    boolean matchesType = "all".equals(type) || type.equalsIgnoreCase(file.getType());
                    boolean matchesQuery = query.isEmpty() ||
                            file.getName().toLowerCase().contains(query.toLowerCase()) ||
                            file.getCaseNumber().toLowerCase().contains(query.toLowerCase());
                    return matchesType && matchesQuery;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(filteredFiles);
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

    // === SUPPORT CHAT BACKEND ===
    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleChatMessage(@RequestParam("message") String message) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Send it using Feign client
            String aiResponse = supportFeignClient.postAiMessage(message);
            response.put("status", "success");
            response.put("response", aiResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // log for debugging
            response.put("status", "error");
            response.put("response", "Sorry, something went wrong.");
            return ResponseEntity.status(500).body(response);
        }
    }

    // Inner class for file information
    public static class FileInfo {
        private String id;
        private String name;
        private String filename;
        private String type;
        private String size;
        private String caseNumber;
        private String dateAdded;
        private boolean temporary = false;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

        public String getDateAdded() { return dateAdded; }
        public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

        public boolean isTemporary() { return temporary; }
        public void setTemporary(boolean temporary) { this.temporary = temporary; }
    }
}
