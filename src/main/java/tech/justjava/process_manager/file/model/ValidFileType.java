package tech.justjava.process_manager.file.model;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;


/**
 * Validate that a valid file ending has been provided.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = ValidFileType.ValidFileTypeValidator.class
)
public @interface ValidFileType {

    String message() default "{file.invalidEnding}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] value();

    class ValidFileTypeValidator implements ConstraintValidator<ValidFileType, FileData> {

        private String[] allowedExtensions;

        @Override
        public void initialize(final ValidFileType constraintAnnotation) {
            this.allowedExtensions = constraintAnnotation.value();
        }

        @Override
        public boolean isValid(final FileData fileData,
                final ConstraintValidatorContext cvContext) {
            if (fileData == null || fileData.getFileName() == null) {
                return true;
            }
            return Arrays.stream(allowedExtensions).anyMatch(extension ->
                    fileData.getFileName().toLowerCase().endsWith("." + extension.toLowerCase()));
        }

    }

}
