package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.CommandeRequest;
import caisse.manager.caisse.dto.KioskSettingsRequest;
import caisse.manager.caisse.dto.KioskSettingsResponse;
import caisse.manager.caisse.dto.KioskUnlockRequest;
import caisse.manager.caisse.dto.LigneCommandeRequest;
import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.Ingredient;
import caisse.manager.caisse.model.LigneCommande;
import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.model.Promotion;
import caisse.manager.caisse.model.Snack;
import caisse.manager.caisse.model.StatutCommande;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.IngredientRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.PromotionRepository;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.security.SnackAccessService;
import caisse.manager.caisse.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin("*")
public class KioskController {

    private final SnackRepository snackRepository;
    private final ProduitRepository produitRepository;
    private final IngredientRepository ingredientRepository;
    private final PromotionRepository promotionRepository;
    private final CommandeRepository commandeRepository;
    private final PromotionService promotionService;
    private final SnackAccessService snackAccessService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @GetMapping("/api/snacks/{id}/kiosk-settings")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getKioskSettings(@PathVariable Long id, Authentication authentication) {
        if (!hasSnackAccess(authentication, id, true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé");
        }

        return snackRepository.findById(id)
                .map(snack -> ResponseEntity.ok(toKioskSettingsResponse(snack)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/api/snacks/{id}/kiosk-settings")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateKioskSettings(
            @PathVariable Long id,
            @RequestBody KioskSettingsRequest request,
            Authentication authentication) {

        if (!hasSnackAccess(authentication, id, true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé");
        }

        Optional<Snack> snackOpt = snackRepository.findById(id);
        if (snackOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Snack snack = snackOpt.get();

        if (request.getEnabled() != null) {
            snack.setKioskEnabled(request.getEnabled());
        }

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String normalizedSlug = normalizeSlug(request.getSlug());
            if (normalizedSlug.length() < 3) {
                return ResponseEntity.badRequest().body("Le slug doit contenir au moins 3 caractères");
            }
            if (snackRepository.existsByKioskSlugAndIdNot(normalizedSlug, snack.getId())) {
                return ResponseEntity.badRequest().body("Ce slug de page borne est déjà utilisé");
            }
            snack.setKioskSlug(normalizedSlug);
        } else if (snack.getKioskSlug() == null || snack.getKioskSlug().isBlank()) {
            snack.setKioskSlug(buildDefaultSlug(snack.getNom(), snack.getId()));
        }

        if (request.getPin() != null && !request.getPin().isBlank()) {
            String pin = request.getPin().trim();
            if (!pin.matches("\\d{4,8}")) {
                return ResponseEntity.badRequest().body("Le code PIN doit contenir entre 4 et 8 chiffres");
            }
            snack.setKioskPinHash(passwordEncoder.encode(pin));
        }

        String generatedPin = null;
        boolean needsPin = Boolean.TRUE.equals(request.getEnabled())
                && (snack.getKioskPinHash() == null || snack.getKioskPinHash().isBlank());
        if (needsPin) {
            generatedPin = generateKioskPin();
            snack.setKioskPinHash(passwordEncoder.encode(generatedPin));
        }

        if (Boolean.TRUE.equals(snack.getKioskEnabled())
                && (snack.getKioskPinHash() == null || snack.getKioskPinHash().isBlank())) {
            return ResponseEntity.badRequest().body("Activez un code PIN avant d'activer la borne");
        }

        snackRepository.save(snack);
        return ResponseEntity.ok(toKioskSettingsResponse(snack, generatedPin));
    }

    @GetMapping("/api/kiosk/{slug}/info")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getKioskInfo(@PathVariable String slug) {
        Optional<Snack> snackOpt = snackRepository.findByKioskSlug(slug.trim().toLowerCase());
        if (snackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Page borne introuvable");
        }

        Snack snack = snackOpt.get();
        if (!Boolean.TRUE.equals(snack.getKioskEnabled())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette borne est désactivée");
        }

        return ResponseEntity.ok(Map.of(
                "snackId", snack.getId(),
                "snackName", snack.getNom(),
                "slug", snack.getKioskSlug()
        ));
    }

    @PostMapping("/api/kiosk/{slug}/unlock")
    @Transactional(readOnly = true)
    public ResponseEntity<?> unlockKiosk(@PathVariable String slug, @RequestBody KioskUnlockRequest request) {
        Optional<Snack> snackOpt = snackRepository.findByKioskSlug(slug.trim().toLowerCase());
        if (snackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Page borne introuvable");
        }

        Snack snack = snackOpt.get();
        if (!Boolean.TRUE.equals(snack.getKioskEnabled())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette borne est désactivée");
        }

        if (!isPinValid(snack, request.getPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Code PIN incorrect");
        }

        return ResponseEntity.ok(Map.of(
                "unlocked", true,
                "snackId", snack.getId(),
                "snackName", snack.getNom()
        ));
    }

    @GetMapping("/api/kiosk/{slug}/products")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getKioskProducts(
            @PathVariable String slug,
            @RequestHeader(value = "X-Kiosk-Pin", required = false) String pin) {

        Optional<Snack> snackOpt = validateKioskAccess(slug, pin);
        if (snackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès borne refusé");
        }

        Long snackId = snackOpt.get().getId();
        List<Produit> produits = produitRepository.findBySnackId(snackId)
                .stream()
                .filter(p -> !Boolean.FALSE.equals(p.getDisponible()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(produits);
    }

    @GetMapping("/api/kiosk/{slug}/ingredients")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getKioskIngredients(
            @PathVariable String slug,
            @RequestHeader(value = "X-Kiosk-Pin", required = false) String pin) {

        Optional<Snack> snackOpt = validateKioskAccess(slug, pin);
        if (snackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès borne refusé");
        }

        Long snackId = snackOpt.get().getId();
        List<Ingredient> ingredients = ingredientRepository.findBySnackId(snackId)
                .stream()
                .filter(i -> !Boolean.FALSE.equals(i.getDisponible()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ingredients);
    }

    @GetMapping("/api/kiosk/{slug}/promotions")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getKioskPromotions(
            @PathVariable String slug,
            @RequestHeader(value = "X-Kiosk-Pin", required = false) String pin) {

        Optional<Snack> snackOpt = validateKioskAccess(slug, pin);
        if (snackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès borne refusé");
        }

        Long snackId = snackOpt.get().getId();
        List<Promotion> promotions = promotionRepository.findActivePromotions(snackId, LocalDate.now());
        return ResponseEntity.ok(promotions);
    }

    @PostMapping("/api/kiosk/{slug}/orders")
    @Transactional
    public ResponseEntity<?> createKioskOrder(
            @PathVariable String slug,
            @RequestHeader(value = "X-Kiosk-Pin", required = false) String pin,
            @RequestBody CommandeRequest request) {

        Optional<Snack> snackOpt = validateKioskAccess(slug, pin);
        if (snackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès borne refusé");
        }

        Long snackId = snackOpt.get().getId();

        if (request.getArticles() == null || request.getArticles().isEmpty()) {
            return ResponseEntity.badRequest().body("La commande ne contient aucun article");
        }

        Commande nouvelleCommande = new Commande();
        nouvelleCommande.setSnackId(snackId);
        nouvelleCommande.setDate(LocalDateTime.now());
        nouvelleCommande.setTypePaiement("EN_CAISSE");
        nouvelleCommande.setStatut(StatutCommande.EN_ATTENTE);

        double totalCalcule = 0.0;
        for (LigneCommandeRequest ligneReq : request.getArticles()) {
            Produit produit = produitRepository.findById(ligneReq.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable"));

            if (!produit.getSnackId().equals(snackId) || Boolean.FALSE.equals(produit.getDisponible())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Produit introuvable pour cette borne");
            }

            LigneCommande ligne = new LigneCommande();
            ligne.setNomProduit(produit.getNom());
            ligne.setQuantite(ligneReq.getQuantite());
            ligne.setDetails(ligneReq.getDetails());

            double prixDeBase = (ligneReq.getPrixFinal() != null) ? ligneReq.getPrixFinal() : produit.getPrix();
            double prixAvecPromotion = promotionService.appliquerPromotionAutomatique(produit, snackId, prixDeBase);

            ligne.setPrixUnitaire(prixAvecPromotion);
            ligne.setCommande(nouvelleCommande);
            nouvelleCommande.getLignes().add(ligne);
            totalCalcule += (prixAvecPromotion * ligneReq.getQuantite());
        }

        Double remise = (request.getRemise() != null && request.getRemise() > 0) ? request.getRemise() : 0.0;
        nouvelleCommande.setRemise(remise);
        nouvelleCommande.setTotal(Math.max(0.0, totalCalcule - remise));

        Commande commandeSauvegardee = commandeRepository.save(nouvelleCommande);
        return ResponseEntity.ok(Map.of(
                "orderId", commandeSauvegardee.getId(),
                "snackId", snackId,
                "total", commandeSauvegardee.getTotal(),
                "paymentMode", "CAISSE",
                "message", "Commande créée. Présentez le numéro en caisse pour payer."
        ));
    }

    private Optional<Snack> validateKioskAccess(String slug, String pin) {
        Optional<Snack> snackOpt = snackRepository.findByKioskSlug(slug.trim().toLowerCase());
        if (snackOpt.isEmpty()) {
            return Optional.empty();
        }

        Snack snack = snackOpt.get();
        if (!Boolean.TRUE.equals(snack.getKioskEnabled())) {
            return Optional.empty();
        }

        if (!isPinValid(snack, pin)) {
            return Optional.empty();
        }
        return Optional.of(snack);
    }

    private boolean isPinValid(Snack snack, String pin) {
        if (pin == null || pin.isBlank()) {
            return false;
        }
        String hash = snack.getKioskPinHash();
        return hash != null && !hash.isBlank() && passwordEncoder.matches(pin.trim(), hash);
    }

    private boolean hasSnackAccess(Authentication authentication, Long snackId, boolean requireManager) {
        return snackAccessService.hasSnackAccess(authentication, snackId, requireManager);
    }

    private String normalizeSlug(String slugInput) {
        String normalized = Normalizer.normalize(slugInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\-\\s]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");

        if (normalized.length() > 64) {
            return normalized.substring(0, 64).replaceAll("-+$", "");
        }
        return normalized;
    }

    private String buildDefaultSlug(String snackName, Long snackId) {
        String base = normalizeSlug(snackName == null ? "snack" : snackName);
        if (base.isBlank()) {
            base = "snack";
        }
        String candidate = base + "-" + snackId;
        if (candidate.length() > 64) {
            candidate = "snack-" + snackId;
        }
        return candidate;
    }

    private String generateKioskPin() {
        int value = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(value);
    }

    private KioskSettingsResponse toKioskSettingsResponse(Snack snack) {
        return toKioskSettingsResponse(snack, null);
    }

    private KioskSettingsResponse toKioskSettingsResponse(Snack snack, String generatedPin) {
        KioskSettingsResponse response = new KioskSettingsResponse();
        response.setSnackId(snack.getId());
        response.setSnackName(snack.getNom());
        response.setEnabled(Boolean.TRUE.equals(snack.getKioskEnabled()));
        response.setSlug(snack.getKioskSlug());
        response.setPinConfigured(snack.getKioskPinHash() != null && !snack.getKioskPinHash().isBlank());
        response.setKioskUrl(snack.getKioskSlug() == null || snack.getKioskSlug().isBlank()
                ? null
                : "/borne/" + snack.getKioskSlug());
        response.setGeneratedPin(generatedPin);
        return response;
    }
}
