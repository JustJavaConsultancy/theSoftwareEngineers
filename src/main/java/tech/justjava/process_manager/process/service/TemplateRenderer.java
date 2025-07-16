package tech.justjava.process_manager.process.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

@Service
public class TemplateRenderer {

    private final TemplateEngine templateEngine;

    public TemplateRenderer() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public String render(String templateString, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateString, context);
    }
}
