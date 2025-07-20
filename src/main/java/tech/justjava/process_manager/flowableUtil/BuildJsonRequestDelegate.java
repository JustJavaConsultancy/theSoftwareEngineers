package tech.justjava.process_manager.flowableUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BuildJsonRequestDelegate implements JavaDelegate {

    private final ObjectMapper objectMapper;

    public BuildJsonRequestDelegate(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userPrompt", execution.getVariable("usAcceptanceCriteria"));

        Map<String, String> metaData = new HashMap<>();
        metaData.put("controllerPackageName", "net.techrunch.ai.controller");
        metaData.put("modelPackageName", "net.techrunch.ai.model");

        payload.put("metaData", metaData);

        String json = null;
        try {
            json = objectMapper.writeValueAsString(payload);
            //System.out.println(" The JSON going to thymeleaf generation==="+json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        execution.setVariable("jsonRepresentation", json);
    }
}
