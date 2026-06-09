package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.CommandeRequest;
import caisse.manager.caisse.dto.EncaissementCommandeRequest;
import caisse.manager.caisse.dto.LigneCommandeRequest;
import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.LigneCommande;
import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.model.StatutCommande;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import caisse.manager.caisse.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Autowired
    private PromotionService promotionService;

    private boolean hasSnackAccess(Authentication authentication, Long snackId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return false;
        }
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(userDetails.getUsername());
        if (utilisateurOpt.isEmpty()) {
            return false;
        }
        Utilisateur utilisateur = utilisateurOpt.get();
        return utilisateur.getSnackId() != null && utilisateur.getSnackId().equals(snackId);
    }

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

            // --- CALCUL DU PRIX AVEC PROMOTION AUTOMATIQUE ---
            // 1. Si le frontend envoie un prix spécifique (ex: avec supplément), on l'utilise comme base
            // 2. Sinon, on prend le prix de base du produit
            double prixDeBase = (ligneReq.getPrixFinal() != null) ? ligneReq.getPrixFinal() : produit.getPrix();
            
            // 3. APPLIQUER AUTOMATIQUEMENT LA PROMOTION (si une promotion active existe pour cette catégorie ou ce produit)
            double prixAvecPromotion = promotionService.appliquerPromotionAutomatique(produit, snackId, prixDeBase);
            
            ligne.setPrixUnitaire(prixAvecPromotion);
            ligne.setCommande(nouvelleCommande);
            nouvelleCommande.getLignes().add(ligne);

            totalCalcule += (prixAvecPromotion * ligneReq.getQuantite());
        }

        // Appliquer la remise si présente
        Double remise = (request.getRemise() != null && request.getRemise() > 0) ? request.getRemise() : 0.0;
        nouvelleCommande.setRemise(remise);
        nouvelleCommande.setTotal(Math.max(0.0, totalCalcule - remise));
        Commande commandeSauvegardee = commandeRepository.save(nouvelleCommande);

        return ResponseEntity.ok("Commande #" + commandeSauvegardee.getId() + " enregistrée !");
    }

    // 1bis. LISTE DES COMMANDES BORNE NON ENCAISSÉES
    @GetMapping("/kiosk/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> getKioskPendingOrders(
            @RequestHeader("X-Snack-ID") Long snackId,
            Authentication authentication) {

        if (!hasSnackAccess(authentication, snackId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé : vous n'avez pas accès à ce restaurant");
        }

        List<Commande> commandes = commandeRepository.findBySnackIdAndTypePaiementAndStatutOrderByDateAsc(
                snackId,
                "EN_CAISSE",
                StatutCommande.EN_ATTENTE
        );

        List<Map<String, Object>> response = commandes.stream().map(c -> Map.of(
                "id", c.getId(),
                "date", c.getDate(),
                "total", c.getTotal() == null ? 0.0 : c.getTotal(),
                "remise", c.getRemise() == null ? 0.0 : c.getRemise(),
                "articlesCount", c.getLignes() == null ? 0 : c.getLignes().size(),
                "lignes", c.getLignes() == null ? List.of() : c.getLignes().stream().map(l -> Map.of(
                        "nomProduit", l.getNomProduit(),
                        "quantite", l.getQuantite(),
                        "prixUnitaire", l.getPrixUnitaire() == null ? 0.0 : l.getPrixUnitaire(),
                        "details", l.getDetails() == null ? "" : l.getDetails()
                )).collect(Collectors.toList())
        )).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 1ter. ENCAISSER UNE COMMANDE BORNE (ESPECES/CARTE)
    @PutMapping("/{id}/encaisser")
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> encaisserCommande(
            @PathVariable Long id,
            @RequestHeader("X-Snack-ID") Long snackId,
            @RequestBody EncaissementCommandeRequest request,
            Authentication authentication) {

        if (!hasSnackAccess(authentication, snackId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé : vous n'avez pas accès à ce restaurant");
        }

        String modePaiement = request.getTypePaiement() == null ? "" : request.getTypePaiement().trim().toUpperCase();
        if (!modePaiement.equals("ESPECES") && !modePaiement.equals("CARTE")) {
            return ResponseEntity.badRequest().body("Type de paiement invalide (ESPECES ou CARTE)");
        }

        Optional<Commande> commandeOpt = commandeRepository.findById(id);
        if (commandeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Commande commande = commandeOpt.get();
        if (!commande.getSnackId().equals(snackId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Commande introuvable pour ce restaurant");
        }

        if (!"EN_CAISSE".equalsIgnoreCase(commande.getTypePaiement())) {
            return ResponseEntity.badRequest().body("Cette commande est déjà encaissée");
        }

        if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
            return ResponseEntity.badRequest().body("Seules les commandes en attente peuvent être encaissées");
        }

        commande.setTypePaiement(modePaiement);
        commandeRepository.save(commande);

        return ResponseEntity.ok(Map.of(
                "id", commande.getId(),
                "typePaiement", commande.getTypePaiement(),
                "message", "Commande encaissée"
        ));
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // Si date == null, utiliser aujourd'hui
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        // Retourner commandes avec statut PRETE ou SERVIE pour la date spécifiée
        return commandeRepository.findBySnackIdAndStatutInAndDateBetweenOrderByDateDesc(
                snackId,
                Arrays.asList(StatutCommande.PRETE, StatutCommande.SERVIE),
                startOfDay,
                endOfDay
        );
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
