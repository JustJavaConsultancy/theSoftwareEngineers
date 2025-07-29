package tech.justjava.process_manager.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tech.justjava.process_manager.keycloak.KeycloakService;

@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {
    private final KeycloakService keycloakService;

    @Override
    public void run(ApplicationArguments args) {
        keycloakService.syncKeycloak();
    }
}
