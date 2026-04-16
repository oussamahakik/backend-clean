package caisse.manager.caisse.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final MyUserDetailsService myUserDetailsService;
    @Value("${app.security.cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

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
                        .requestMatchers("/api/promotions").hasAnyRole("MANAGER", "SUPER_ADMIN")
                        .requestMatchers("/api/promotions/**").hasAnyRole("MANAGER", "SUPER_ADMIN")
                        .requestMatchers("/api/snacks/*/info").authenticated()
                        .requestMatchers("/api/snacks/*/settings").hasRole("MANAGER")
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

        // 1. L'URL exacte de ton frontend sur Render (en dur, sans le slash à la fin)
        configuration.setAllowedOrigins(Arrays.asList("https://caisse-manager-ui.onrender.com"));

        // 2. On autorise toutes les méthodes classiques
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 3. On autorise absolument tous les headers (avec "*") pour éviter les blocages de pré-vérification
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 4. On autorise l'envoi des credentials (cookies, headers d'auth)
        configuration.setAllowCredentials(true);

        // 5. On expose le header Authorization pour que ton frontend Vue.js puisse lire le token JWT
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 6. TRÈS IMPORTANT : On applique ça sur "/**" (absolument tout le projet) et non plus juste "/api/**"
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
