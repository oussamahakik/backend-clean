package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.CommandeRequest;
import caisse.manager.caisse.dto.LigneCommandeRequest;
import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.LigneCommande;
import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.model.StatutCommande;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/commandes")
@CrossOrigin("*")
public class CommandeController {

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 1. CRÉER UNE COMMANDE
    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> creerCommande(
            @RequestBody CommandeRequest request,
            @RequestHeader("X-Snack-ID") Long snackId,
            Authentication authentication) {

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

        Commande nouvelleCommande = new Commande();
        nouvelleCommande.setSnackId(snackId);
        nouvelleCommande.setDate(LocalDateTime.now());
        nouvelleCommande.setTypePaiement(request.getTypePaiement());
        nouvelleCommande.setStatut(StatutCommande.EN_ATTENTE);

        double totalCalcule = 0.0;

        for (LigneCommandeRequest ligneReq : request.getArticles()) {
            Produit produit = produitRepository.findById(ligneReq.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable"));
            
            // VÉRIFIER que le produit appartient au snackId
            if (!produit.getSnackId().equals(snackId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Produit introuvable ou n'appartient pas à ce restaurant");
            }

            LigneCommande ligne = new LigneCommande();
            ligne.setNomProduit(produit.getNom());
            ligne.setQuantite(ligneReq.getQuantite());
            ligne.setDetails(ligneReq.getDetails());

            // --- MODIFICATION ICI ---
            // Si le frontend envoie un prix spécifique (ex: avec supplément), on l'utilise.
            // Sinon, on prend le prix de base.
            double prixUnitaire = (ligneReq.getPrixFinal() != null) ? ligneReq.getPrixFinal() : produit.getPrix();

            ligne.setPrixUnitaire(prixUnitaire);
            ligne.setCommande(nouvelleCommande);
            nouvelleCommande.getLignes().add(ligne);

            totalCalcule += (prixUnitaire * ligneReq.getQuantite());
        }

        // Appliquer la remise si présente
        Double remise = (request.getRemise() != null && request.getRemise() > 0) ? request.getRemise() : 0.0;
        nouvelleCommande.setRemise(remise);
        nouvelleCommande.setTotal(Math.max(0.0, totalCalcule - remise));
        Commande commandeSauvegardee = commandeRepository.save(nouvelleCommande);

        return ResponseEntity.ok("Commande #" + commandeSauvegardee.getId() + " enregistrée !");
    }

    // 2. COMMANDES ACTIVES (CUISINE)
    @GetMapping("/actives")
    @PreAuthorize("hasRole('MANAGER')")
    public List<Commande> getCommandesActives(@RequestHeader("X-Snack-ID") Long snackId) {
        return commandeRepository.findBySnackIdAndStatut(snackId, StatutCommande.EN_ATTENTE);
    }

    // 3. HISTORIQUE DES COMMANDES (PRÊTES OU SERVIES)
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MANAGER', 'ROLE_MANAGER')")
    public List<Commande> getCommandesHistorique(
            @RequestHeader("X-Snack-ID") Long snackId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        // Si date == null, utiliser aujourd'hui
        if (date == null) {
            date = java.time.LocalDate.now();
        }
        // Retourner commandes avec statut PRETE ou SERVIE pour la date spécifiée
        return commandeRepository.findBySnackIdAndStatutAndDate(snackId, date);
    }

    // 4. CHANGER STATUT
    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateStatut(
            @PathVariable Long id,
            @RequestParam StatutCommande nouveauStatut,
            @RequestHeader("X-Snack-ID") Long snackId) {

        return commandeRepository.findById(id).map(commande -> {
            if (!commande.getSnackId().equals(snackId)) {
                return ResponseEntity.status(403).body("Interdit");
            }
            commande.setStatut(nouveauStatut);
            commandeRepository.save(commande);
            return ResponseEntity.ok("Statut mis à jour");
        }).orElse(ResponseEntity.notFound().build());
    }
}