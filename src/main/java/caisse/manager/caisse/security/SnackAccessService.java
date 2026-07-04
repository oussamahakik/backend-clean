package caisse.manager.caisse.security;

import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SnackAccessService {

    private final UtilisateurRepository utilisateurRepository;

    public boolean isSuperAdmin(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_SUPER_ADMIN", "SUPER_ADMIN");
    }

    public boolean isManager(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_MANAGER", "MANAGER");
    }

    public boolean hasSnackAccess(Authentication authentication, Long snackId, boolean requireManager) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (requireManager && !isManager(authentication)) {
            return false;
        }

        return getAuthenticatedUser(authentication)
                .map(Utilisateur::getSnackId)
                .filter(snackId::equals)
                .isPresent();
    }

    public Optional<Utilisateur> getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return Optional.empty();
        }
        return utilisateurRepository.findByUsername(userDetails.getUsername());
    }

    private boolean hasAuthority(Authentication authentication, String... allowedAuthorities) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> {
                    for (String allowedAuthority : allowedAuthorities) {
                        if (allowedAuthority.equals(authority)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}
