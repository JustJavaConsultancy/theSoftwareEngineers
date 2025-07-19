package tech.justjava.process_manager.process.form;

import org.springframework.stereotype.Component;

@Component
public class FormMapper {

    public FormDTO toDTO(Form form) {
        if (form == null) return null;

        FormDTO dto = new FormDTO();
        dto.setId(form.getId());
        dto.setFormName(form.getFormName());
        dto.setFormCode(form.getFormCode());
        dto.setFormDetails(form.getFormDetails());
        dto.setFormInterface(form.getFormInterface());

        return dto;
    }

    public Form toEntity(FormDTO dto) {
        if (dto == null) return null;

        Form form = new Form();
        form.setId(dto.getId());
        form.setFormName(dto.getFormName());
        form.setFormCode(dto.getFormCode());
        form.setFormDetails(dto.getFormDetails());
        form.setFormInterface(dto.getFormInterface());

        return form;
    }

    public void updateEntity(FormDTO dto, Form form) {
        form.setFormName(dto.getFormName());
        form.setFormCode(dto.getFormCode());
        form.setFormDetails(dto.getFormDetails());
        form.setFormInterface(dto.getFormInterface());
    }
}
