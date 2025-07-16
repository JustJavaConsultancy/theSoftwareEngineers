package tech.justjava.process_manager.file.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode
public class FileData {

    @NotNull
    @Size(max = 255)
    private String uid;

    @NotNull
    @Size(max = 255)
    private String fileName;

}
