package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.*;
import caisse.manager.caisse.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin("*")
@RequiredArgsConstructor
public class UtilisateurController {
    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<UtilisateurDTO>> getAllUtilisateurs(
        @RequestHeader("X-Snack-ID") Long snackId
    ) {
        return ResponseEntity.ok(utilisateurService.getAllBySnackId(snackId));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> createUtilisateur(
        @RequestHeader("X-Snack-ID") Long snackId,
        @RequestBody CreateUtilisateurRequest request
    ) {
        try {
            return ResponseEntity.ok(utilisateurService.create(request, snackId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateUtilisateur(
        @PathVariable Long id,
        @RequestHeader("X-Snack-ID") Long snackId,
        @RequestBody UpdateUtilisateurRequest request
    ) {
        try {
            return ResponseEntity.ok(utilisateurService.update(id, request, snackId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteUtilisateur(
        @PathVariable Long id,
        @RequestHeader("X-Snack-ID") Long snackId
    ) {
        try {
            utilisateurService.delete(id, snackId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> resetPassword(
        @PathVariable Long id,
        @RequestHeader("X-Snack-ID") Long snackId
    ) {
        try {
            String tempPassword = utilisateurService.resetPassword(id, snackId);
            return ResponseEntity.ok(tempPassword);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> toggleStatus(
        @PathVariable Long id,
        @RequestHeader("X-Snack-ID") Long snackId
    ) {
        try {
            return ResponseEntity.ok(utilisateurService.toggleStatus(id, snackId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

