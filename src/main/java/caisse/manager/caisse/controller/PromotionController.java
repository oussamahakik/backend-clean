package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.PromotionDTO;
import caisse.manager.caisse.model.Promotion;
import caisse.manager.caisse.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin("*")
public class PromotionController {

    @Autowired
    private PromotionRepository promotionRepository;

    @GetMapping
    public ResponseEntity<?> getPromotions(@RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            List<Promotion> promotions;
            if (snackId != null) {
                promotions = promotionRepository.findBySnackIdOrderByDateCreationDesc(snackId);
            } else {
                promotions = promotionRepository.findAll();
            }
            return ResponseEntity.ok(promotions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la récupération des promotions: " + e.getMessage());
        }
    }

    @GetMapping("/actives")
    public ResponseEntity<?> getPromotionsActives(@RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            if (snackId == null) {
                return ResponseEntity.badRequest().body("Snack ID requis");
            }
            List<Promotion> promotions = promotionRepository.findBySnackIdAndActifTrue(snackId);
            // Filtrer par date
            LocalDate now = LocalDate.now();
            promotions = promotions.stream()
                .filter(p -> !p.getDateDebut().isAfter(now) && !p.getDateFin().isBefore(now))
                .collect(Collectors.toList());
            return ResponseEntity.ok(promotions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la récupération des promotions: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> createPromotion(@RequestBody PromotionDTO dto, 
                                             @RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Promotion promotion = new Promotion();
            promotion.setNom(dto.getNom());
            promotion.setDescription(dto.getDescription());
            promotion.setTypePromotion(dto.getTypePromotion());
            promotion.setValeur(dto.getValeur());
            promotion.setDateDebut(dto.getDateDebut());
            promotion.setDateFin(dto.getDateFin());
            promotion.setCodePromo(dto.getCodePromo());
            promotion.setActif(dto.getActif() != null ? dto.getActif() : true);
            promotion.setProduitId(dto.getProduitId());
            promotion.setCategorie(dto.getCategorie());
            promotion.setSnackId(snackId != null ? snackId : dto.getSnackId());
            promotion.setNombreUtilisationsMax(dto.getNombreUtilisationsMax());
            
            promotion = promotionRepository.save(promotion);
            return ResponseEntity.ok(promotion);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la création de la promotion: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> updatePromotion(@PathVariable Long id, @RequestBody PromotionDTO dto) {
        try {
            Optional<Promotion> promotionOpt = promotionRepository.findById(id);
            if (promotionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Promotion promotion = promotionOpt.get();
            if (dto.getNom() != null) promotion.setNom(dto.getNom());
            if (dto.getDescription() != null) promotion.setDescription(dto.getDescription());
            if (dto.getTypePromotion() != null) promotion.setTypePromotion(dto.getTypePromotion());
            if (dto.getValeur() != null) promotion.setValeur(dto.getValeur());
            if (dto.getDateDebut() != null) promotion.setDateDebut(dto.getDateDebut());
            if (dto.getDateFin() != null) promotion.setDateFin(dto.getDateFin());
            if (dto.getCodePromo() != null) promotion.setCodePromo(dto.getCodePromo());
            if (dto.getActif() != null) promotion.setActif(dto.getActif());
            if (dto.getProduitId() != null) promotion.setProduitId(dto.getProduitId());
            if (dto.getCategorie() != null) promotion.setCategorie(dto.getCategorie());
            if (dto.getNombreUtilisationsMax() != null) promotion.setNombreUtilisationsMax(dto.getNombreUtilisationsMax());
            
            promotion = promotionRepository.save(promotion);
            return ResponseEntity.ok(promotion);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la mise à jour de la promotion: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> deletePromotion(@PathVariable Long id) {
        try {
            if (!promotionRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            promotionRepository.deleteById(id);
            return ResponseEntity.ok("Promotion supprimée avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la suppression de la promotion: " + e.getMessage());
        }
    }
}


