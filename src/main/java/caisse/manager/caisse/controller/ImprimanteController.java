package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.ImprimanteDTO;
import caisse.manager.caisse.model.Imprimante;
import caisse.manager.caisse.repository.ImprimanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/imprimantes")
@CrossOrigin("*")
public class ImprimanteController {

    @Autowired
    private ImprimanteRepository imprimanteRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getImprimantes(@RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            List<Imprimante> imprimantes;
            if (snackId != null) {
                imprimantes = imprimanteRepository.findBySnackIdOrderByNomAsc(snackId);
            } else {
                imprimantes = imprimanteRepository.findAll();
            }
            return ResponseEntity.ok(imprimantes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la récupération des imprimantes: " + e.getMessage());
        }
    }

    @GetMapping("/actives")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getImprimantesActives(@RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            if (snackId == null) {
                return ResponseEntity.badRequest().body("Snack ID requis");
            }
            List<Imprimante> imprimantes = imprimanteRepository.findBySnackIdAndActifTrue(snackId);
            return ResponseEntity.ok(imprimantes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la récupération des imprimantes: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> createImprimante(@RequestBody ImprimanteDTO dto,
                                               @RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Imprimante imprimante = new Imprimante();
            imprimante.setNom(dto.getNom());
            imprimante.setType(dto.getType());
            imprimante.setChemin(dto.getChemin());
            imprimante.setSnackId(snackId != null ? snackId : dto.getSnackId());
            imprimante.setActif(dto.getActif() != null ? dto.getActif() : true);
            imprimante.setDescription(dto.getDescription());
            imprimante.setLargeurPapier(dto.getLargeurPapier() != null ? dto.getLargeurPapier() : 80);
            imprimante.setCopies(dto.getCopies() != null ? dto.getCopies() : 1);
            imprimante.setImpressionAuto(dto.getImpressionAuto() != null ? dto.getImpressionAuto() : false);
            imprimante.setTypeTicket(dto.getTypeTicket() != null ? dto.getTypeTicket() : "COMMANDE");
            
            imprimante = imprimanteRepository.save(imprimante);
            return ResponseEntity.ok(imprimante);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la création de l'imprimante: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateImprimante(@PathVariable Long id, @RequestBody ImprimanteDTO dto) {
        try {
            Optional<Imprimante> imprimanteOpt = imprimanteRepository.findById(id);
            if (imprimanteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Imprimante imprimante = imprimanteOpt.get();
            if (dto.getNom() != null) imprimante.setNom(dto.getNom());
            if (dto.getType() != null) imprimante.setType(dto.getType());
            if (dto.getChemin() != null) imprimante.setChemin(dto.getChemin());
            if (dto.getActif() != null) imprimante.setActif(dto.getActif());
            if (dto.getDescription() != null) imprimante.setDescription(dto.getDescription());
            if (dto.getLargeurPapier() != null) imprimante.setLargeurPapier(dto.getLargeurPapier());
            if (dto.getCopies() != null) imprimante.setCopies(dto.getCopies());
            if (dto.getImpressionAuto() != null) imprimante.setImpressionAuto(dto.getImpressionAuto());
            if (dto.getTypeTicket() != null) imprimante.setTypeTicket(dto.getTypeTicket());
            imprimante.setDateModification(LocalDateTime.now());
            
            imprimante = imprimanteRepository.save(imprimante);
            return ResponseEntity.ok(imprimante);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la mise à jour de l'imprimante: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteImprimante(@PathVariable Long id) {
        try {
            if (!imprimanteRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            imprimanteRepository.deleteById(id);
            return ResponseEntity.ok("Imprimante supprimée avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la suppression de l'imprimante: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> testImprimante(@PathVariable Long id) {
        try {
            Optional<Imprimante> imprimanteOpt = imprimanteRepository.findById(id);
            if (imprimanteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Imprimante imprimante = imprimanteOpt.get();
            // TODO: Implémenter le test d'impression réel
            // Pour l'instant, on retourne juste un message de succès
            return ResponseEntity.ok("Test d'impression envoyé à l'imprimante: " + imprimante.getNom());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors du test de l'imprimante: " + e.getMessage());
        }
    }
}


