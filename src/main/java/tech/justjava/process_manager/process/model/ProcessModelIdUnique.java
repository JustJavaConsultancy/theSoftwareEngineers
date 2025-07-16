package tech.justjava.process_manager.process.model;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.web.servlet.HandlerMapping;
import tech.justjava.process_manager.process.service.ProcessService;


/**
 * Validate that the modelId value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = ProcessModelIdUnique.ProcessModelIdUniqueValidator.class
)
public @interface ProcessModelIdUnique {

    String message() default "{Exists.process.modelId}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProcessModelIdUniqueValidator implements ConstraintValidator<ProcessModelIdUnique, String> {

        private final ProcessService processService;
        private final HttpServletRequest request;

        public ProcessModelIdUniqueValidator(final ProcessService processService,
                final HttpServletRequest request) {
            this.processService = processService;
            this.request = request;
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked") final Map<String, String> pathVariables =
                    ((Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null && value.equalsIgnoreCase(processService.get(Long.parseLong(currentId)).getModelId())) {
                // value hasn't changed
                return true;
            }
            return !processService.modelIdExists(value);
        }

    }

}
