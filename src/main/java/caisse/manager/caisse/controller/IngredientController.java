package caisse.manager.caisse.controller;

import caisse.manager.caisse.model.Ingredient;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.IngredientRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@CrossOrigin("*") // Décommenter si vous avez des soucis CORS en local malgré la configyes
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 1. LISTER LES INGRÉDIENTS (Par Snack)
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> getIngredients(
            @RequestHeader("X-Snack-ID") Long snackId,
            Authentication authentication) {
        
        try {
            if (snackId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("X-Snack-ID header manquant");
            }
            
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
            
            List<Ingredient> ingredients = ingredientRepository.findBySnackId(snackId);
            return ResponseEntity.ok(ingredients != null ? ingredients : java.util.Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la récupération des ingrédients: " + e.getMessage());
        }
    }

    // 2. AJOUTER UN INGRÉDIENT
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Ingredient addIngredient(
            @RequestBody Ingredient ingredient,
            @RequestHeader("X-Snack-ID") Long snackId) {

        ingredient.setSnackId(snackId);
        // Par sécurité, on s'assure qu'il est disponible à la création
        if (ingredient.getDisponible() == null) {
            ingredient.setDisponible(true);
        }
        // Si le prix supplément n'est pas mis, on met 0.0
        if (ingredient.getPrixSupplement() == null) {
            ingredient.setPrixSupplement(0.0);
        }

        return ingredientRepository.save(ingredient);
    }

    // 3. CHANGER LA DISPONIBILITÉ (ON/OFF)
    @PutMapping("/{id}/dispo")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> toggleDisponibilite(
            @PathVariable Long id,
            @RequestHeader("X-Snack-ID") Long snackId) {

        return ingredientRepository.findById(id).map(ing -> {
            // SÉCURITÉ : On vérifie que l'ingrédient appartient au snack
            if (!ing.getSnackId().equals(snackId)) {
                return ResponseEntity.status(403).body("Interdit");
            }

            boolean actuel = ing.getDisponible() != null ? ing.getDisponible() : false;
            ing.setDisponible(!actuel);

            ingredientRepository.save(ing);
            return ResponseEntity.ok(ing);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. SUPPRIMER UN INGRÉDIENT
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteIngredient(
            @PathVariable Long id,
            @RequestHeader("X-Snack-ID") Long snackId) {

        return ingredientRepository.findById(id).map(ing -> {
            if (!ing.getSnackId().equals(snackId)) {
                return ResponseEntity.status(403).body("Interdit");
            }
            ingredientRepository.delete(ing);
            return ResponseEntity.ok("Ingrédient supprimé");
        }).orElse(ResponseEntity.notFound().build());
    }
}