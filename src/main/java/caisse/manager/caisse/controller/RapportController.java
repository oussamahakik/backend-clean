package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.RapportDTO;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import caisse.manager.caisse.service.RapportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rapports")
@CrossOrigin("*")
public class RapportController {

    @Autowired
    private RapportService rapportService;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getRapport(
            @RequestHeader("X-Snack-ID") Long snackId,
            @RequestParam(defaultValue = "MOIS") String periode,
            Authentication authentication) {
        
        // VÉRIFIER que l'utilisateur authentifié appartient bien au snackId
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            
            Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElse(null);
            
            if (utilisateur != null && utilisateur.getSnackId() != null) {
                if (!utilisateur.getSnackId().equals(snackId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Accès refusé : vous n'avez pas accès à ce restaurant");
                }
            }
        }
        
        RapportDTO rapport = rapportService.getStatistiques(snackId, periode);
        return ResponseEntity.ok(rapport);
    }
}

