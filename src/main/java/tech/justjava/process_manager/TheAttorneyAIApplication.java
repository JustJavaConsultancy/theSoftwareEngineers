package tech.justjava.process_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients
public class TheAttorneyAIApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TheAttorneyAIApplication.class, args);
    }

}
