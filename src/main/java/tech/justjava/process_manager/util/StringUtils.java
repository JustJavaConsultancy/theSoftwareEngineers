package tech.justjava.process_manager.util;
import org.springframework.stereotype.Component;

@Component("stringUtils")  // The name here is important!
public class StringUtils {

    public String camelToWords(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        String withSpaces = camelCase
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        return withSpaces.substring(0, 1).toUpperCase() + withSpaces.substring(1);
    }
    public int getPositiveHashCode(String input) {
        if (input == null) return 0;
        return input.hashCode() & 0x7fffffff;
    }
}
