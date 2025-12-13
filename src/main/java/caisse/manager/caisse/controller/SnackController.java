package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.SnackConfigDTO;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/snacks")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class SnackController {
    private final SnackRepository snackRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * GET /api/snacks/{id}/settings
     * Récupère la configuration complète d'un snack (identité + préférences).
     * Accessible uniquement aux MANAGER ayant accès à ce snack ou SUPER_ADMIN.
     */
    @GetMapping("/{id}/settings")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<SnackConfigDTO> getSettings(
            @PathVariable Long id,
            @RequestHeader(value = "X-Snack-ID", required = false) Long snackIdHeader,
            Authentication authentication) {

        try {
            log.info("Tentative de récupération des paramètres du snack avec ID {}", id);
            
            // Vérification de l'accès
            if (!checkAccess(authentication, id, true)) {
                log.warn("Accès refusé pour le snack {}.", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return snackRepository.findById(id)
                .map(s -> {
                    log.info("Snack trouvé, récupération des paramètres.");
                    SnackConfigDTO dto = new SnackConfigDTO();
                    // Mapping manuel des données du snack vers le DTO
                    dto.setNom(s.getNom() != null ? s.getNom() : "");
                    dto.setAdresse(s.getAdresse() != null ? s.getAdresse() : "");
                    dto.setTelephone(s.getTelephone() != null ? s.getTelephone() : "");
                    dto.setEmail(s.getEmail() != null ? s.getEmail() : "");
                    dto.setSiteWeb(s.getSiteWeb() != null ? s.getSiteWeb() : "");
                    dto.setThemeColor(s.getThemeColor() != null ? s.getThemeColor() : "light");
                    dto.setNotifications(s.getNotificationsEnabled() != null ? s.getNotificationsEnabled() : true);
                    dto.setPrintAuto(s.getPrintAuto() != null ? s.getPrintAuto() : false);
                    dto.setCurrency(s.getCurrency() != null ? s.getCurrency() : "EUR");

                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    log.warn("Snack avec ID {} non trouvé.", id);
                    return ResponseEntity.notFound().build();
                });
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des paramètres du snack {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/snacks/{id}/settings
     * Sauvegarde la configuration complète.
     * RESTRICTED: Accessible uniquement au SUPER_ADMIN pour garantir la sécurité des données métier.
     */
    @PutMapping("/{id}/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateSettings(
            @PathVariable Long id,
            @RequestBody SnackConfigDTO dto,
            Authentication authentication) {

        try {
            log.info("Tentative de mise à jour des paramètres du snack avec ID {}", id);
            
            // Le contrôle d'accès SUPER_ADMIN est déjà fait par @PreAuthorize, 
            // mais on vérifie quand même la cohérence si besoin ou on log.
            // On peut réutiliser checkAccess qui gère le SUPER_ADMIN.
            if (!checkAccess(authentication, id, false)) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return snackRepository.findById(id).map(s -> {
                log.info("Snack trouvé, mise à jour des paramètres.");
                // Mise à jour des champs
                if (dto.getNom() != null) s.setNom(dto.getNom());
                if (dto.getAdresse() != null) s.setAdresse(dto.getAdresse());
                if (dto.getTelephone() != null) s.setTelephone(dto.getTelephone());
                if (dto.getEmail() != null) s.setEmail(dto.getEmail());
                if (dto.getSiteWeb() != null) s.setSiteWeb(dto.getSiteWeb());
                if (dto.getThemeColor() != null) s.setThemeColor(dto.getThemeColor());
                if (dto.getNotifications() != null) s.setNotificationsEnabled(dto.getNotifications());
                if (dto.getPrintAuto() != null) s.setPrintAuto(dto.getPrintAuto());
                if (dto.getCurrency() != null) s.setCurrency(dto.getCurrency());

                snackRepository.save(s);
                log.info("Les paramètres du snack {} ont été mis à jour avec succès.", id);

                return ResponseEntity.ok(dto);
            }).orElseGet(() -> {
                log.warn("Snack avec ID {} non trouvé pour mise à jour.", id);
                return ResponseEntity.notFound().build();
            });
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des paramètres du snack {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/snacks/{id}/info
     * Endpoint INFO simple pour le header (App.js).
     * Accessible à tous les utilisateurs authentifiés du snack.
     */
    @GetMapping("/{id}/info")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInfo(@PathVariable Long id, Authentication authentication) {
        try {
            log.info("Tentative de récupération des informations du snack {}", id);
            
            // Vérification de l'accès (pas besoin d'être manager)
            if (!checkAccess(authentication, id, false)) {
                log.warn("Accès refusé pour le snack {}.", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return snackRepository.findById(id)
                .map(s -> {
                    log.info("Snack trouvé, récupération des informations.");
                    return ResponseEntity.ok(Map.of(
                        "nom", s.getNom() != null ? s.getNom() : "Mon Snack",
                        "adresse", s.getAdresse() != null ? s.getAdresse() : "",
                        "telephone", s.getTelephone() != null ? s.getTelephone() : ""
                    ));
                })
                .orElseGet(() -> {
                    log.warn("Snack avec ID {} non trouvé.", id);
                    return ResponseEntity.notFound().build();
                });
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des infos du snack {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Méthode utilitaire privée pour vérifier l'accès
     */
    private boolean checkAccess(Authentication authentication, Long snackId, boolean requireManager) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("L'utilisateur n'est pas authentifié. Accès refusé.");
            return false;
        }

        // SUPER_ADMIN a toujours accès
        boolean isSuperAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_SUPER_ADMIN") || auth.equals("SUPER_ADMIN"));
        
        if (isSuperAdmin) {
            log.info("L'utilisateur est un SUPER_ADMIN, accès autorisé.");
            return true;
        }

        // Vérifier le rôle MANAGER si requis
        if (requireManager) {
            boolean isManager = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_MANAGER") || auth.equals("MANAGER"));
            if (!isManager) {
                log.warn("L'utilisateur n'a pas le rôle MANAGER, accès refusé.");
                return false;
            } else {
                log.info("L'utilisateur est un MANAGER, accès autorisé.");
            }
        }

        // Vérifier l'appartenance au snack
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(username);
            if (utilisateurOpt.isPresent()) {
                Utilisateur utilisateur = utilisateurOpt.get();
                if (utilisateur.getSnackId() != null && utilisateur.getSnackId().equals(snackId)) {
                    log.info("L'utilisateur {} a accès au snack {}", username, snackId);
                    return true;
                } else {
                    log.warn("L'utilisateur {} n'a pas accès au snack {}", username, snackId);
                }
            }
        }

        log.warn("Accès refusé au snack {} pour l'utilisateur.", snackId);
        return false;
    }
}
