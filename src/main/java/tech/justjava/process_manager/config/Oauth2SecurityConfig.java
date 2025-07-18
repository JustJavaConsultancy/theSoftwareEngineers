package tech.justjava.process_manager.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AnonymousConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class Oauth2SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(Oauth2SecurityConfig.class);
    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        log.debug("Configuring security");


        http.anonymous(AnonymousConfigurer::disable)
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer
                                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .csrf(CsrfConfigurer::disable)
                .oauth2Login(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authorize -> authorize
                                .requestMatchers(new MvcRequestMatcher(introspector, "/login"))
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )

                .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler()));
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler=
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
        return oidcLogoutSuccessHandler;
    }
}
