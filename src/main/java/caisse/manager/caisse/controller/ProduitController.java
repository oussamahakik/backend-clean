package caisse.manager.caisse.controller;

import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class ProduitController {

    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;

    // GET http://localhost:8081/api/produits
    // On attend un Header "X-Snack-ID" pour savoir quel menu charger
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> getMenu(
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
                    // Vérifier que l'utilisateur appartient au snackId demandé
                    if (!utilisateur.getSnackId().equals(snackId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Accès refusé : vous n'avez pas accès à ce restaurant");
                    }
                }
            }
            
            log.info("Chargement du menu pour le snack n°{}", snackId);
            List<Produit> produits = produitRepository.findBySnackId(snackId);
            return ResponseEntity.ok(produits != null ? produits : java.util.Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la récupération des produits: " + e.getMessage());
        }
    }

    // POST http://localhost:8081/api/produits (Pour ajouter un produit - utile pour tester)
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> ajouterProduit(
            @RequestBody Produit produitRequest, 
            @RequestHeader("X-Snack-ID") Long snackId) {
        
        if (snackId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("X-Snack-ID header manquant");
        }
        
        // Créer un nouveau produit pour éviter tout problème avec la désérialisation
        // CRITIQUE : Toujours créer un nouveau Produit avec le snackId depuis le header
        Produit produit = new Produit();
        produit.setNom(produitRequest.getNom());
        produit.setPrix(produitRequest.getPrix());
        produit.setCategorie(produitRequest.getCategorie());
        produit.setImage(produitRequest.getImage());
        produit.setSnackId(snackId); // FORCER le snackId depuis le header (sécurité)
        
        // Si disponible n'est pas défini, on le met à true par défaut
        produit.setDisponible(produitRequest.getDisponible() != null ? produitRequest.getDisponible() : true);
        
        // Vérification finale : le snackId doit absolument être défini
        if (produit.getSnackId() == null) {
            log.error("Erreur critique : snackId est null avant la sauvegarde ! Header snackId: {}", snackId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur interne : snackId non défini");
        }
        
        try {
            log.info("Création du produit '{}' pour le snackId: {}", produit.getNom(), produit.getSnackId());
            Produit produitSauvegarde = produitRepository.save(produit);
            log.info("Produit créé avec succès, ID: {}, snackId: {}", produitSauvegarde.getId(), produitSauvegarde.getSnackId());
            return ResponseEntity.ok(produitSauvegarde);
        } catch (Exception e) {
            log.error("Erreur lors de la création du produit '{}' pour snackId {}: {}", 
                    produit.getNom(), produit.getSnackId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la création du produit: " + e.getMessage());
        }
    }

    // 3. MODIFIER un produit (PUT)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateProduit(
            @PathVariable Long id,
            @RequestBody Produit produitModifie,
            @RequestHeader("X-Snack-ID") Long snackId) {

        if (snackId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("X-Snack-ID header manquant");
        }

        return produitRepository.findById(id).map(produit -> {
            // SÉCURITÉ : On vérifie que le produit appartient bien au snack connecté !
            if (produit.getSnackId() == null || !produit.getSnackId().equals(snackId)) {
                return ResponseEntity.status(403).body("Ce produit ne vous appartient pas.");
            }

            produit.setNom(produitModifie.getNom());
            produit.setPrix(produitModifie.getPrix());
            produit.setCategorie(produitModifie.getCategorie());
            // produit.setImage(produitModifie.getImage());

            produitRepository.save(produit);
            return ResponseEntity.ok(produit);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. SUPPRIMER un produit (DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteProduit(
            @PathVariable Long id,
            @RequestHeader("X-Snack-ID") Long snackId) {

        if (snackId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("X-Snack-ID header manquant");
        }

        return produitRepository.findById(id).map(produit -> {
            // SÉCURITÉ
            if (produit.getSnackId() == null || !produit.getSnackId().equals(snackId)) {
                return ResponseEntity.status(403).body("Interdit.");
            }
            produitRepository.delete(produit);
            return ResponseEntity.ok("Produit supprimé.");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. CHANGER LA DISPONIBILITÉ (ON/OFF)
    @PutMapping("/{id}/dispo")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> toggleDisponibilite(
            @PathVariable Long id,
            @RequestHeader("X-Snack-ID") Long snackId) {

        if (snackId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("X-Snack-ID header manquant");
        }

        return produitRepository.findById(id).map(produit -> {
            // SÉCURITÉ : Vérifier que le produit appartient au bon snack
            if (produit.getSnackId() == null || !produit.getSnackId().equals(snackId)) {
                return ResponseEntity.status(403).body("Interdit");
            }

            // Bascule (Vrai devient Faux, Faux devient Vrai)
            // Note: Assurez-vous que votre entité Produit a bien un champ 'disponible' (Boolean)
            // Si 'getDisponible()' retourne null, on considère que c'est false par défaut
            boolean actuel = produit.getDisponible() != null ? produit.getDisponible() : false;
            produit.setDisponible(!actuel);

            produitRepository.save(produit);
            return ResponseEntity.ok(produit);
        }).orElse(ResponseEntity.notFound().build());
    }
}