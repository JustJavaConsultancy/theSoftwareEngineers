package tech.justjava.process_manager.util;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
    public static String InstantToStringDate(Instant instant) {
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
        LocalDate messageDate = dateTime.toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a");
        if (messageDate.equals(today)) {
            return "Today, " + dateTime.format(timeFormatter);
        } else if (messageDate.equals(today.minusDays(1))) {
            return "Yesterday, " + dateTime.format(timeFormatter);
        } else {
            return dateTime.format(fullFormatter);
        }
    }
}
