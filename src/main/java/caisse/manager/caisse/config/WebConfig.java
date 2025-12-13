package caisse.manager.caisse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // La configuration CORS est maintenant gérée dans SecurityConfig.java
    // pour éviter les conflits avec Spring Security
}

