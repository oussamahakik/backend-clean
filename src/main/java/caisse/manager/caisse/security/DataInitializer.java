package caisse.manager.caisse.security;

import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.bootstrap.super-admin.enabled:true}")
    private boolean superAdminEnabled;
    @Value("${app.bootstrap.super-admin.username:hakik_owner}")
    private String superAdminUsername;
    @Value("${app.bootstrap.super-admin.password:ChangeMe12345!}")
    private String superAdminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!superAdminEnabled) {
            log.info("--- INITIALISATION DU SUPER ADMIN DESACTIVEE ---");
            return;
        }

        if (utilisateurRepository.findByUsername(superAdminUsername).isEmpty()) {
            log.info("--- INITIALISATION DU SUPER ADMIN ---");

            Utilisateur superAdmin = new Utilisateur();
            superAdmin.setUsername(superAdminUsername);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));

            superAdmin.setRole("SUPER_ADMIN");
            superAdmin.setSnackId(null); // Pas de snack spécifique, il voit tout

            utilisateurRepository.save(superAdmin);
            log.info("--- SUPER ADMIN CRÉÉ ---");
        }
    }
}
