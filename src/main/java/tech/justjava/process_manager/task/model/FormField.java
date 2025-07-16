package tech.justjava.process_manager.task.model;

import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode
public class FormField {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String label;

    @Size(max = 255)
    private String fieldType;

}
