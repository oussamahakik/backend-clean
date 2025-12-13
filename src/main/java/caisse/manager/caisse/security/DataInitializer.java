package caisse.manager.caisse.security;

import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // On vérifie si le SUPER ADMIN existe (Vous !)
        // Changez "hakik_owner" par le pseudo que vous voulez utiliser
        if (utilisateurRepository.findByUsername("hakik_owner").isEmpty()) {
            log.info("--- INITIALISATION DU SUPER ADMIN ---");

            Utilisateur superAdmin = new Utilisateur();
            superAdmin.setUsername("hakik_owner");

            // DÉFINISSEZ VOTRE MOT DE PASSE MAÎTRE ICI
            superAdmin.setPassword(passwordEncoder.encode("oussamahakikOwner"));

            superAdmin.setRole("SUPER_ADMIN");
            superAdmin.setSnackId(null); // Pas de snack spécifique, il voit tout

            utilisateurRepository.save(superAdmin);
            log.info("--- SUPER ADMIN CRÉÉ ---");
        }
    }
}