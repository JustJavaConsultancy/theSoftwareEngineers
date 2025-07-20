package tech.justjava.process_manager.flowableUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PrepareInitialVariableDelegate implements JavaDelegate {
    private final ObjectMapper objectMapper;

    public PrepareInitialVariableDelegate(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userPrompt", execution.getVariable("userPrompt"));

        try {
            String json = objectMapper.writeValueAsString(payload);
            execution.setVariable("userStory", json);
            //System.out.println(" The JSON going to thymeleaf generation==="+json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
