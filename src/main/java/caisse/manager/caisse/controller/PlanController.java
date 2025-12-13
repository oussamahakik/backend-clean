package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.PlanDTO;
import caisse.manager.caisse.model.Plan;
import caisse.manager.caisse.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin("*")
public class PlanController {

    @Autowired
    private PlanRepository planRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllPlans() {
        try {
            List<Plan> plans = planRepository.findAll();
            return ResponseEntity.ok(plans != null ? plans : java.util.Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur lors de la récupération des plans", 
                            "message", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createPlan(@RequestBody PlanDTO dto) {
        try {
            // Validation
            if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le nom du plan est requis"));
            }
            if (dto.getPrixMensuel() == null || dto.getPrixMensuel() < 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le prix mensuel doit être positif"));
            }

            Plan plan = new Plan();
            plan.setNom(dto.getNom().trim());
            plan.setPrixMensuel(dto.getPrixMensuel());
            plan.setDescription(dto.getDescription());
            plan.setNombreRestaurantsMax(dto.getNombreRestaurantsMax());
            plan.setNombreUtilisateursMax(dto.getNombreUtilisateursMax());
            plan.setActif(dto.getActif() != null ? dto.getActif() : true);
            
            plan = planRepository.save(plan);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur lors de la création du plan", 
                            "message", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @RequestBody PlanDTO dto) {
        try {
            Optional<Plan> planOpt = planRepository.findById(id);
            if (planOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Plan plan = planOpt.get();
            if (dto.getNom() != null) plan.setNom(dto.getNom());
            if (dto.getPrixMensuel() != null) plan.setPrixMensuel(dto.getPrixMensuel());
            if (dto.getDescription() != null) plan.setDescription(dto.getDescription());
            if (dto.getNombreRestaurantsMax() != null) plan.setNombreRestaurantsMax(dto.getNombreRestaurantsMax());
            if (dto.getNombreUtilisateursMax() != null) plan.setNombreUtilisateursMax(dto.getNombreUtilisateursMax());
            if (dto.getActif() != null) plan.setActif(dto.getActif());
            
            plan = planRepository.save(plan);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la mise à jour du plan: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        try {
            if (!planRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            planRepository.deleteById(id);
            return ResponseEntity.ok("Plan supprimé avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la suppression du plan: " + e.getMessage());
        }
    }
}

