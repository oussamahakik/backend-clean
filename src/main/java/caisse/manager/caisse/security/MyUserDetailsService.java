package caisse.manager.caisse.security;

import caisse.manager.caisse.model.Snack;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final SnackRepository snackRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        // --- VÉRIFICATION DU STATUT DU SNACK ---
        // Ne pas vérifier pour SUPER_ADMIN (pas de snackId)
        if (utilisateur.getSnackId() != null) {
            Snack snack = snackRepository.findById(utilisateur.getSnackId())
                    .orElseThrow(() -> new UsernameNotFoundException("Snack introuvable"));

            if (!snack.isActif()) {
                // Si le snack est désactivé, on bloque le login
                throw new DisabledException("Votre restaurant a été suspendu. Contactez l'administrateur.");
            }

            // Vérification de l'abonnement (seulement si le snack a un plan)
            if (snack.getPlan() != null) {
                if (snack.getDateFinAbonnement() == null || snack.getDateFinAbonnement().isBefore(LocalDate.now())) {
                    throw new CredentialsExpiredException("Abonnement expiré. Veuillez contacter le support.");
                }
            }
        }
        // ---------------------------------------

        // Ajouter le préfixe ROLE_ pour Spring Security
        String role = utilisateur.getRole();
        if (role != null && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

        return new User(
                utilisateur.getUsername(),
                utilisateur.getPassword(),
                Collections.singletonList(authority)
        );
    }
}