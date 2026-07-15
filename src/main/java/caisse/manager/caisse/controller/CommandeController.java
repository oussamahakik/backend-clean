package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.CommandeRequest;
import caisse.manager.caisse.dto.EncaissementCommandeRequest;
import caisse.manager.caisse.dto.LigneCommandeRequest;
import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.LigneCommande;
import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.model.StatutCommande;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.security.SnackAccessService;
import caisse.manager.caisse.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/commandes")
@CrossOrigin("*")
@RequiredArgsConstructor
public class CommandeController {

    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final PromotionService promotionService;
    private final SnackAccessService snackAccessService;

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> creerCommande(
            @RequestBody CommandeRequest request,
            @RequestHeader("X-Snack-ID") Long snackId,
            Authentication authentication) {

        if (!snackAccessService.hasSnackAccess(authentication, snackId, false)) {
            return forbiddenSnackAccess();
        }

        if (request.getArticles() == null || request.getArticles().isEmpty()) {
            return ResponseEntity.badRequest().body("La commande ne contient aucun article");
        }

        Commande nouvelleCommande = new Commande();
        nouvelleCommande.setSnackId(snackId);
        nouvelleCommande.setDate(LocalDateTime.now());
        nouvelleCommande.setTypePaiement(request.getTypePaiement());
        nouvelleCommande.setStatut(StatutCommande.EN_ATTENTE);

        double totalCalcule = 0.0;
        for (LigneCommandeRequest ligneReq : request.getArticles()) {
            Produit produit = findProductForSnack(snackId, ligneReq.getProduitId());
            if (produit == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Produit introuvable ou n'appartient pas à ce restaurant");
            }

            LigneCommande ligne = buildOrderLine(nouvelleCommande, produit, ligneReq, snackId);
            totalCalcule += ligne.getPrixUnitaire() * ligne.getQuantite();
        }

        nouvelleCommande.setRemise(resolveRemise(request.getRemise()));
        nouvelleCommande.setTotal(Math.max(0.0, totalCalcule - nouvelleCommande.getRemise()));

        Commande commandeSauvegardee = commandeRepository.save(nouvelleCommande);
        return ResponseEntity.ok("Commande #" + commandeSauvegardee.getId() + " enregistrée !");
    }

    @GetMapping("/kiosk/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> getKioskPendingOrders(
            @RequestHeader("X-Snack-ID") Long snackId,
            Authentication authentication) {

        if (!snackAccessService.hasSnackAccess(authentication, snackId, false)) {
            return forbiddenSnackAccess();
        }

        List<Map<String, Object>> response = commandeRepository
                .findBySnackIdAndTypePaiementAndStatutOrderByDateAsc(
                        snackId,
                        "EN_CAISSE",
                        StatutCommande.EN_ATTENTE
                )
                .stream()
                .map(this::mapPendingOrder)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/encaisser")
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'CAISSIER')")
    public ResponseEntity<?> encaisserCommande(
            @PathVariable Long id,
            @RequestHeader("X-Snack-ID") Long snackId,
            @RequestBody EncaissementCommandeRequest request,
            Authentication authentication) {

        if (!snackAccessService.hasSnackAccess(authentication, snackId, false)) {
            return forbiddenSnackAccess();
        }

        String modePaiement = normalizePaymentMode(request.getTypePaiement());
        if (!isAllowedPaymentMode(modePaiement)) {
            return ResponseEntity.badRequest().body("Type de paiement invalide (ESPECES ou CARTE)");
        }

        Optional<Commande> commandeOpt = commandeRepository.findById(id);
        if (commandeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Commande commande = commandeOpt.get();
        if (!snackId.equals(commande.getSnackId())) {
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

    @GetMapping("/actives")
    @PreAuthorize("hasRole('MANAGER')")
    public List<Commande> getCommandesActives(@RequestHeader("X-Snack-ID") Long snackId) {
        return commandeRepository.findBySnackIdAndStatut(snackId, StatutCommande.EN_ATTENTE);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MANAGER', 'ROLE_MANAGER')")
    public List<Commande> getCommandesHistorique(
            @RequestHeader("X-Snack-ID") Long snackId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        LocalDateTime startOfDay = effectiveDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return commandeRepository.findBySnackIdAndStatutInAndDateBetweenOrderByDateDesc(
                snackId,
                List.of(StatutCommande.PRETE, StatutCommande.SERVIE),
                startOfDay,
                endOfDay
        );
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateStatut(
            @PathVariable Long id,
            @RequestParam StatutCommande nouveauStatut,
            @RequestHeader("X-Snack-ID") Long snackId) {

        return commandeRepository.findById(id).map(commande -> {
            if (!snackId.equals(commande.getSnackId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Interdit");
            }
            commande.setStatut(nouveauStatut);
            commandeRepository.save(commande);
            return ResponseEntity.ok("Statut mis à jour");
        }).orElse(ResponseEntity.notFound().build());
    }

    private LigneCommande buildOrderLine(
            Commande commande,
            Produit produit,
            LigneCommandeRequest ligneReq,
            Long snackId) {

        LigneCommande ligne = new LigneCommande();
        ligne.setNomProduit(produit.getNom());
        ligne.setQuantite(ligneReq.getQuantite());
        ligne.setDetails(ligneReq.getDetails());

        double prixDeBase = ligneReq.getPrixFinal() != null ? ligneReq.getPrixFinal() : produit.getPrix();
        double prixAvecPromotion = promotionService.appliquerPromotionAutomatique(produit, snackId, prixDeBase);

        ligne.setPrixUnitaire(prixAvecPromotion);
        ligne.setCommande(commande);
        commande.getLignes().add(ligne);
        return ligne;
    }

    private Produit findProductForSnack(Long snackId, Long produitId) {
        return produitRepository.findById(produitId)
                .filter(produit -> snackId.equals(produit.getSnackId()))
                .orElse(null);
    }

    private Map<String, Object> mapPendingOrder(Commande commande) {
        return Map.of(
                "id", commande.getId(),
                "date", commande.getDate(),
                "total", safeDouble(commande.getTotal()),
                "remise", safeDouble(commande.getRemise()),
                "articlesCount", commande.getLignes() == null ? 0 : commande.getLignes().size(),
                "lignes", commande.getLignes() == null ? List.of() : commande.getLignes().stream().map(l -> Map.of(
                        "nomProduit", l.getNomProduit(),
                        "quantite", l.getQuantite(),
                        "prixUnitaire", safeDouble(l.getPrixUnitaire()),
                        "details", l.getDetails() == null ? "" : l.getDetails()
                )).collect(Collectors.toList())
        );
    }

    private ResponseEntity<String> forbiddenSnackAccess() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Accès refusé : vous n'avez pas accès à ce restaurant");
    }

    private Double resolveRemise(Double remise) {
        return remise != null && remise > 0 ? remise : 0.0;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private String normalizePaymentMode(String paymentMode) {
        return paymentMode == null ? "" : paymentMode.trim().toUpperCase();
    }

    private boolean isAllowedPaymentMode(String modePaiement) {
        return "ESPECES".equals(modePaiement) || "CARTE".equals(modePaiement);
    }
}
