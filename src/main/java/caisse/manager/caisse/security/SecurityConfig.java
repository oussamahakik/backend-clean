package caisse.manager.caisse.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final MyUserDetailsService myUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF (inutile avec JWT)
                .csrf(csrf -> csrf.disable())
                // Activer CORS (important pour React)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // GESTION DES ACCÈS
                .authorizeHttpRequests(auth -> auth
                        // Autoriser les requêtes Pre-flight CORS (OPTIONS) pour éviter l'erreur PatternParseException
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll() // Double vérification pour être sûr
                        .requestMatchers("/api/super-admin/**").hasAuthority("ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/diagnostic/**").hasAuthority("ROLE_SUPER_ADMIN") // Endpoints de diagnostic
                        .requestMatchers("/api/utilisateurs/**").hasRole("MANAGER")
                        .requestMatchers("/api/rapports/**").hasRole("MANAGER")
                        .requestMatchers("/api/produits").hasAnyRole("MANAGER", "CAISSIER")
                        .requestMatchers("/api/produits/**").hasRole("MANAGER")
                        .requestMatchers("/api/ingredients/**").hasRole("MANAGER")
                        .requestMatchers("/api/commandes").hasAnyRole("MANAGER", "CAISSIER")
                        .requestMatchers("/api/commandes/**").hasAnyRole("MANAGER", "CAISSIER")
                        .requestMatchers("/api/snacks/**/info").authenticated()
                        .requestMatchers("/api/snacks/**/settings").hasRole("MANAGER")
                        .anyRequest().authenticated()
                )

                // Pas de session (Stateless) car on utilise des Tokens
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Ajouter notre filtre avant celui par défaut
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Le standard pour crypter les mots de passe
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(myUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "X-Snack-ID", "Accept", "Origin", 
            "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
