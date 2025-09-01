package tech.justjava.process_manager.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component("env")
public class EnvironmentManager {
    private final Environment environment;

    public EnvironmentManager(Environment environment) {
        this.environment = environment;
    }
    public String get(String key){
        System.out.println(" The Returning URL ==="+environment.getProperty(key) +
                " key==="+key);
        return environment.getProperty(key);
    }
}
