package tech.justjava.process_manager.process.form;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    Optional<Form> findByFormName(String formName);

    Optional<Form> findByFormCode(String formCode);

}