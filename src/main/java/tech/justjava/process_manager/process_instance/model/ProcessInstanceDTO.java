package tech.justjava.process_manager.process_instance.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ProcessInstanceDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String processName;

    @Size(max = 255)
    private String businessKey;

    private Map<String, String> processVariable;

    private Status status;

    private Long process;

}
