package tech.justjava.process_manager.gallery;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource; import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient (name = "file-service", url = "https://genaiandrag.onrender.com")
public interface FileFeignClient {
        @GetMapping("/download/{id}")
        ResponseEntity<Resource > downloadFile(@PathVariable("id") String id);
        @DeleteMapping("/delete/{id}")
        ResponseEntity<String> deleteFile(@PathVariable("id") String id);
        @PostMapping(value = "/uploadWithMetaData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        String uploadWithMetaData(@RequestPart("file") MultipartFile file, @RequestPart("metadata") String metadata);

}
