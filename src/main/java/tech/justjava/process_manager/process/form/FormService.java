package tech.justjava.process_manager.process.form;

import java.util.List;
import java.util.Optional;

public interface FormService {
    List<Form> findAll();
    Optional<Form> findById(Long id);
    Optional<Form> findByFormName(String formName);
    Optional<Form> findByFormCode(String formCode);
    Form save(Form form);
    void deleteById(Long id);
}
