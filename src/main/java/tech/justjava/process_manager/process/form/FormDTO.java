package tech.justjava.process_manager.process.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormDTO {
    private Long id;
    private String formName;
    private String formCode;
    private String formDetails;
    private String formInterface;
}
