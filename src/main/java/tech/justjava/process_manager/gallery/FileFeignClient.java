package tech.justjava.process_manager.gallery;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource; import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@FeignClient (name = "file-service", url = "https://genaiandrag.onrender.com")
public interface FileFeignClient {
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file);
@GetMapping("/download/{id}")
ResponseEntity<Resource > downloadFile(@PathVariable("id") String id);
@DeleteMapping("/delete/{id}")
ResponseEntity<String> deleteFile(@PathVariable("id") String id);
}