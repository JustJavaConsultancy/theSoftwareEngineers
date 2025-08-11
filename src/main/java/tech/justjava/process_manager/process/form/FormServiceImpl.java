package tech.justjava.process_manager.process.form;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FormServiceImpl implements FormService {

    private final FormRepository formRepository;

    public FormServiceImpl(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    @Override
    public List<Form> findAll() {
        return formRepository.findAll();
    }

    @Override
    public List<Form> findByProcessKey(String processKey) {
        return formRepository.findByProcessKey(processKey);
    }

    @Override
    public Optional<Form> findById(Long id) {
        return formRepository.findById(id);
    }

    @Override
    public Optional<Form> findByFormName(String formName) {
        return formRepository.findByFormName(formName);
    }

    @Override
    public Optional<Form> findByFormCode(String formCode) {
        return formRepository.findByFormCode(formCode);
    }

    @Override
    public Form save(Form form) {
        //System.out.println(" The form name=="+form.getFormName());
        //System.out.println(" The form code=="+form.getFormCode());
        //System.out.println(" The form details=="+form.getFormDetails());
        //System.out.println("  The form interface=="+form.getFormInterface());
        //System.out.println(" The form process key=="+form.getProcessKey());
        return formRepository.save(form);
    }

    @Override
    public void deleteById(Long id) {
        formRepository.deleteById(id);
    }
}
