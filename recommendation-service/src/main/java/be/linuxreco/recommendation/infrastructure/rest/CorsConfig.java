package be.linuxreco.recommendation.infrastructure.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS.
 *
 * Le navigateur refuse par defaut qu'une page servie par une origine
 * appelle une API d'une autre origine. Il faut donc autoriser
 * explicitement l'origine de notre interface.
 *
 * JAMAIS "*" : cela autoriserait n'importe quel site à appeler cette
 * API depuis le navigateur de n'importe quel visiteur. L'origine
 * autorisée vient d'une variable d'environnement, differente en
 * developpement et en production.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigin;

    public CorsConfig(@Value("${cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST");
    }
}
