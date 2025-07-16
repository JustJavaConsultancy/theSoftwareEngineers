package tech.justjava.process_manager.task.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TaskDTO {

    private Long id;

    @Size(max = 255)
    private String taskName;

    @Size(max = 255)
    private String taskForm;

    private Map<String, String> taskVariable;

    @Valid
    private List<FormField> fields;

    @NotNull
    private TaskStatus taskStatus;

    private Long processInstance;

}
