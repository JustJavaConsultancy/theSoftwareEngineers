package tech.justjava.process_manager.support;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "supportFeignClient", url="https://genaiandrag.onrender.com")
public interface SupportFeignClient {
    @PostMapping("/support")
   String postAiMessage(@RequestBody String request);

    @PostMapping("/generateLegalDocument")
    public String generateLegalDocument(@RequestBody Map<String,String> legalRequest);
}
