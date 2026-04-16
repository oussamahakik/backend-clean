package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.AbonnementDTO;
import caisse.manager.caisse.dto.CreateSnackRequest;
import caisse.manager.caisse.dto.PlanDTO;
import caisse.manager.caisse.dto.SnackUpdateRequest;
import caisse.manager.caisse.dto.SuperAdminSnackDTO;
import caisse.manager.caisse.dto.UpdateManagerRequest;
import caisse.manager.caisse.dto.UtilisateurSuperAdminDTO;
import caisse.manager.caisse.model.Plan;
import caisse.manager.caisse.model.Snack;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.IngredientRepository;
import caisse.manager.caisse.repository.PlanRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/super-admin")
@CrossOrigin("*")
public class SuperAdminController {

    @Autowired
    private SnackRepository snackRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private PlanRepository planRepository;

    @GetMapping("/snacks")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SuperAdminSnackDTO>> getAllSnacks() {
        List<SuperAdminSnackDTO> snacks = snackRepository.findAll().stream()
                .map(this::toSuperAdminSnackDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(snacks);
    }

    @PostMapping("/snacks")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> createSnack(@RequestBody CreateSnackRequest request) {
        if (request.getNomRestaurant() == null || request.getNomRestaurant().isBlank()) {
            return ResponseEntity.badRequest().body("Le nom du restaurant est requis.");
        }
        if (request.getUsernameManager() == null || request.getUsernameManager().isBlank()) {
            return ResponseEntity.badRequest().body("Le nom d'utilisateur manager est requis.");
        }
        if (request.getPasswordManager() == null || request.getPasswordManager().isBlank()) {
            return ResponseEntity.badRequest().body("Le mot de passe manager est requis.");
        }
        if (utilisateurRepository.findByUsername(request.getUsernameManager().trim()).isPresent()) {
            return ResponseEntity.badRequest().body("Ce nom d'utilisateur manager existe déjà.");
        }

        Snack snack = new Snack();
        snack.setNom(request.getNomRestaurant().trim());
        snack.setAdresse(request.getAdresse());
        snack = snackRepository.save(snack);

        Utilisateur manager = new Utilisateur();
        manager.setUsername(request.getUsernameManager().trim());
        manager.setPassword(passwordEncoder.encode(request.getPasswordManager()));
        manager.setRole("MANAGER");
        manager.setSnackId(snack.getId());
        manager.setActif(true);
        manager.setDateCreation(java.time.LocalDateTime.now());
        utilisateurRepository.save(manager);

        return ResponseEntity.ok("Restaurant '" + snack.getNom() + "' créé avec succès (ID: " + snack.getId() + ")");
    }

    @PutMapping("/snacks/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateSnack(@PathVariable Long id, @RequestBody SnackUpdateRequest request) {
        return snackRepository.findById(id).map(snack -> {
            if (request.getNom() != null && !request.getNom().trim().isEmpty()) {
                snack.setNom(request.getNom().trim());
            }
            if (request.getAdresse() != null) {
                snack.setAdresse(request.getAdresse());
            }
            snackRepository.save(snack);
            return ResponseEntity.ok(toSuperAdminSnackDTO(snack));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/snacks/{id}/manager")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateManager(@PathVariable Long id, @RequestBody UpdateManagerRequest request) {
        Snack snack = snackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Snack non trouvé"));

        Optional<Utilisateur> managerOpt = utilisateurRepository.findBySnackIdAndRole(snack.getId(), "ROLE_MANAGER");
        if (managerOpt.isEmpty()) {
            managerOpt = utilisateurRepository.findBySnackIdAndRole(snack.getId(), "MANAGER");
        }
        if (managerOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Aucun manager trouvé pour ce restaurant.");
        }

        Utilisateur manager = managerOpt.get();

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String username = request.getUsername().trim();
            Optional<Utilisateur> existingUser = utilisateurRepository.findByUsername(username);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(manager.getId())) {
                return ResponseEntity.badRequest().body("Ce nom d'utilisateur existe déjà.");
            }
            manager.setUsername(username);
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            manager.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        utilisateurRepository.save(manager);
        return ResponseEntity.ok("Manager modifié avec succès.");
    }

    @PutMapping("/snacks/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleSnackStatus(@PathVariable Long id) {
        return snackRepository.findById(id).map(snack -> {
            boolean nouveauStatut = !snack.isActif();
            snack.setActif(nouveauStatut);
            snackRepository.save(snack);
            return ResponseEntity.ok(nouveauStatut ? "Restaurant réactivé." : "Restaurant suspendu.");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/snacks/{id}")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteSnack(@PathVariable Long id) {
        if (!snackRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        utilisateurRepository.deleteBySnackId(id);
        produitRepository.findBySnackId(id).forEach(produitRepository::delete);
        ingredientRepository.findBySnackId(id).forEach(ingredientRepository::delete);
        commandeRepository.findBySnackIdOrderByDateDesc(id).forEach(commandeRepository::delete);
        snackRepository.deleteById(id);

        return ResponseEntity.ok("Restaurant et toutes ses données supprimés définitivement.");
    }

    @PostMapping("/snacks/{snackId}/reset-manager-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> resetManagerPassword(@PathVariable Long snackId) {
        Optional<Utilisateur> managerOpt = utilisateurRepository.findBySnackIdAndRole(snackId, "ROLE_MANAGER");
        if (managerOpt.isEmpty()) {
            managerOpt = utilisateurRepository.findBySnackIdAndRole(snackId, "MANAGER");
        }

        if (managerOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Aucun manager trouvé pour ce restaurant.");
        }

        Utilisateur manager = managerOpt.get();

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder tempPassword = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            tempPassword.append(chars.charAt(random.nextInt(chars.length())));
        }
        String tempPasswordStr = tempPassword.toString();

        manager.setPassword(passwordEncoder.encode(tempPasswordStr));
        utilisateurRepository.save(manager);

        return ResponseEntity.ok(tempPasswordStr);
    }

    @PutMapping("/snacks/{id}/abonnement")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> gererAbonnement(@PathVariable Long id, @RequestBody AbonnementDTO dto) {
        return snackRepository.findById(id).map(snack -> {
            if (dto.getPlanId() == null) {
                snack.setPlan(null);
            } else {
                Plan plan = planRepository.findById(dto.getPlanId())
                        .orElseThrow(() -> new RuntimeException("Plan introuvable"));
                if (Boolean.FALSE.equals(plan.getActif())) {
                    throw new RuntimeException("Ce plan est inactif et ne peut pas être attribué");
                }
                snack.setPlan(plan);
            }

            if (dto.getDateFinAbonnement() != null && !dto.getDateFinAbonnement().isBlank()) {
                try {
                    snack.setDateFinAbonnement(LocalDate.parse(dto.getDateFinAbonnement()));
                } catch (DateTimeParseException e) {
                    throw new RuntimeException("Format date fin abonnement invalide (YYYY-MM-DD)");
                }
            } else {
                snack.setDateFinAbonnement(null);
            }

            snackRepository.save(snack);
            return ResponseEntity.ok("Abonnement mis à jour avec succès.");
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UtilisateurSuperAdminDTO>> getAllUsers() {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();

        Map<Long, String> snackNames = snackRepository.findAll().stream()
                .collect(Collectors.toMap(Snack::getId, Snack::getNom, (a, b) -> a));

        List<UtilisateurSuperAdminDTO> dtos = utilisateurs.stream()
                .map(u -> {
                    UtilisateurSuperAdminDTO dto = new UtilisateurSuperAdminDTO();
                    dto.setId(u.getId());
                    dto.setUsername(u.getUsername());
                    dto.setRole(u.getRole());
                    dto.setActif(u.getActif() != null ? u.getActif() : true);
                    dto.setSnackId(u.getSnackId());
                    dto.setDateCreation(u.getDateCreation());
                    dto.setSnackNom(u.getSnackId() != null ? snackNames.getOrDefault(u.getSnackId(), "Restaurant inconnu") : "Super Admin");
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/users/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Long>> getUsersCount() {
        return ResponseEntity.ok(Map.of("count", utilisateurRepository.count()));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(id);

        if (utilisateurOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur utilisateur = utilisateurOpt.get();

        if ("SUPER_ADMIN".equals(utilisateur.getRole()) || "ROLE_SUPER_ADMIN".equals(utilisateur.getRole())) {
            long superAdminCount = utilisateurRepository.findAll().stream()
                    .filter(u -> "SUPER_ADMIN".equals(u.getRole()) || "ROLE_SUPER_ADMIN".equals(u.getRole()))
                    .count();

            if (superAdminCount <= 1) {
                return ResponseEntity.badRequest().body("Impossible de supprimer le dernier Super Admin.");
            }
        }

        utilisateurRepository.delete(utilisateur);
        return ResponseEntity.ok("Utilisateur supprimé avec succès.");
    }

    private SuperAdminSnackDTO toSuperAdminSnackDTO(Snack snack) {
        SuperAdminSnackDTO dto = new SuperAdminSnackDTO();
        dto.setId(snack.getId());
        dto.setNom(snack.getNom());
        dto.setAdresse(snack.getAdresse());
        dto.setTelephone(snack.getTelephone());
        dto.setEmail(snack.getEmail());
        dto.setSiteWeb(snack.getSiteWeb());
        dto.setDateCreation(snack.getDateCreation());
        dto.setActif(snack.isActif());
        dto.setDateFinAbonnement(snack.getDateFinAbonnement());

        if (snack.getPlan() != null) {
            PlanDTO planDTO = new PlanDTO();
            planDTO.setId(snack.getPlan().getId());
            planDTO.setNom(snack.getPlan().getNom());
            planDTO.setPrixMensuel(snack.getPlan().getPrixMensuel());
            planDTO.setDescription(snack.getPlan().getDescription());
            planDTO.setNombreRestaurantsMax(snack.getPlan().getNombreRestaurantsMax());
            planDTO.setNombreUtilisateursMax(snack.getPlan().getNombreUtilisateursMax());
            planDTO.setActif(snack.getPlan().getActif());
            dto.setPlan(planDTO);
        }

        return dto;
    }
}
