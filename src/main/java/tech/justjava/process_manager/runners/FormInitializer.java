package tech.justjava.process_manager.runners;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tech.justjava.process_manager.process.form.Form;
import tech.justjava.process_manager.process.form.FormRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FormInitializer implements ApplicationRunner {
    private final FormRepository formRepository;


    public FormInitializer(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:forms/*");

        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            fileName=fileName.substring(0, fileName.lastIndexOf('.'));
            if (fileName == null) continue;

            // Extract code from filename (without extension)
            String code = fileName.split("-")[0];
            String processKey = fileName.split("-")[1];

            // Check if form with this code already exists
            if (formRepository.findByFormCode(code).isPresent()) {
                System.out.println("Form with code " + code + " already exists, skipping...");
                continue;
            }

            // Read file content
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String content = reader.lines()
                        .collect(java.util.stream.Collectors.joining("\n"));

                // Create and save Form entity
                Form form = new Form();
                form.setFormCode(code);
                form.setFormInterface(content);
                form.setProcessKey(processKey);
                formRepository.save(form);
                System.out.println("Loaded and saved form: " + code);
            } catch (Exception e) {
                System.err.println("Error loading file " + fileName + ": " + e.getMessage());
            }
        }
    }

}
