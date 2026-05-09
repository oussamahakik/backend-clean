package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.ImprimanteDTO;
import caisse.manager.caisse.model.Imprimante;
import caisse.manager.caisse.repository.ImprimanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            Long effectiveSnackId = resolveSnackIdOrThrow(snackId, null);
            List<Imprimante> imprimantes = imprimanteRepository.findBySnackIdOrderByNomAsc(effectiveSnackId);
            return ResponseEntity.ok(imprimantes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la récupération des imprimantes: " + e.getMessage());
        }
    }

    @GetMapping("/actives")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getImprimantesActives(@RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Long effectiveSnackId = resolveSnackIdOrThrow(snackId, null);
            List<Imprimante> imprimantes = imprimanteRepository.findBySnackIdAndActifTrue(effectiveSnackId);
            return ResponseEntity.ok(imprimantes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la récupération des imprimantes: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> createImprimante(@RequestBody ImprimanteDTO dto,
                                               @RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Long effectiveSnackId = resolveSnackIdOrThrow(snackId, dto.getSnackId());
            validatePayload(dto);

            Imprimante imprimante = new Imprimante();
            imprimante.setNom(dto.getNom().trim());
            imprimante.setType(dto.getType().trim().toUpperCase());
            imprimante.setChemin(dto.getChemin().trim());
            imprimante.setSnackId(effectiveSnackId);
            imprimante.setActif(dto.getActif() != null ? dto.getActif() : true);
            imprimante.setDescription(dto.getDescription());
            imprimante.setLargeurPapier(dto.getLargeurPapier() != null ? dto.getLargeurPapier() : 80);
            imprimante.setCopies(dto.getCopies() != null ? dto.getCopies() : 1);
            imprimante.setImpressionAuto(dto.getImpressionAuto() != null ? dto.getImpressionAuto() : false);
            imprimante.setTypeTicket(dto.getTypeTicket() != null ? dto.getTypeTicket().trim().toUpperCase() : "COMMANDE");
            imprimante.setDateCreation(LocalDateTime.now());
            imprimante.setDateModification(LocalDateTime.now());

            imprimante = imprimanteRepository.save(imprimante);
            return ResponseEntity.ok(imprimante);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la création de l'imprimante: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateImprimante(@PathVariable Long id,
                                               @RequestBody ImprimanteDTO dto,
                                               @RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Optional<Imprimante> imprimanteOpt = imprimanteRepository.findById(id);
            if (imprimanteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Imprimante imprimante = imprimanteOpt.get();
            assertAccessToPrinter(imprimante, snackId);

            if (dto.getNom() != null && !dto.getNom().isBlank()) imprimante.setNom(dto.getNom().trim());
            if (dto.getType() != null && !dto.getType().isBlank()) imprimante.setType(dto.getType().trim().toUpperCase());
            if (dto.getChemin() != null && !dto.getChemin().isBlank()) imprimante.setChemin(dto.getChemin().trim());
            if (dto.getActif() != null) imprimante.setActif(dto.getActif());
            if (dto.getDescription() != null) imprimante.setDescription(dto.getDescription());
            if (dto.getLargeurPapier() != null) imprimante.setLargeurPapier(dto.getLargeurPapier());
            if (dto.getCopies() != null) imprimante.setCopies(dto.getCopies());
            if (dto.getImpressionAuto() != null) imprimante.setImpressionAuto(dto.getImpressionAuto());
            if (dto.getTypeTicket() != null && !dto.getTypeTicket().isBlank()) imprimante.setTypeTicket(dto.getTypeTicket().trim().toUpperCase());
            validatePayload(dto);
            imprimante.setDateModification(LocalDateTime.now());

            imprimante = imprimanteRepository.save(imprimante);
            return ResponseEntity.ok(imprimante);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la mise à jour de l'imprimante: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteImprimante(@PathVariable Long id,
                                               @RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Optional<Imprimante> imprimanteOpt = imprimanteRepository.findById(id);
            if (imprimanteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            assertAccessToPrinter(imprimanteOpt.get(), snackId);
            imprimanteRepository.deleteById(id);
            return ResponseEntity.ok("Imprimante supprimée avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la suppression de l'imprimante: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> testImprimante(@PathVariable Long id,
                                             @RequestHeader(value = "X-Snack-ID", required = false) Long snackId) {
        try {
            Optional<Imprimante> imprimanteOpt = imprimanteRepository.findById(id);
            if (imprimanteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Imprimante imprimante = imprimanteOpt.get();
            assertAccessToPrinter(imprimante, snackId);

            return ResponseEntity.ok("Test d'impression envoyé à l'imprimante: " + imprimante.getNom());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors du test de l'imprimante: " + e.getMessage());
        }
    }

    private void validatePayload(ImprimanteDTO dto) {
        if (dto.getNom() != null && dto.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'imprimante est requis");
        }
        if (dto.getType() != null && dto.getType().isBlank()) {
            throw new IllegalArgumentException("Le type d'imprimante est requis");
        }
        if (dto.getChemin() != null && dto.getChemin().isBlank()) {
            throw new IllegalArgumentException("Le chemin de l'imprimante est requis");
        }
        if (dto.getCopies() != null && dto.getCopies() < 1) {
            throw new IllegalArgumentException("Le nombre de copies doit être supérieur ou égal à 1");
        }
        if (dto.getLargeurPapier() != null && dto.getLargeurPapier() <= 0) {
            throw new IllegalArgumentException("La largeur du papier doit être supérieure à 0");
        }
    }

    private Long resolveSnackIdOrThrow(Long snackIdHeader, Long snackIdBody) {
        if (isSuperAdmin()) {
            Long resolved = snackIdHeader != null ? snackIdHeader : snackIdBody;
            if (resolved == null) {
                throw new IllegalArgumentException("Snack ID requis");
            }
            return resolved;
        }
        if (snackIdHeader == null) {
            throw new IllegalArgumentException("Snack ID requis");
        }
        return snackIdHeader;
    }

    private void assertAccessToPrinter(Imprimante imprimante, Long snackIdHeader) {
        if (isSuperAdmin()) {
            return;
        }
        if (snackIdHeader != null && !snackIdHeader.equals(imprimante.getSnackId())) {
            throw new IllegalArgumentException("Accès refusé à cette imprimante");
        }
    }

    private boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
