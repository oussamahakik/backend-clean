package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.CreateSnackRequest;
import caisse.manager.caisse.dto.SnackUpdateRequest;
import caisse.manager.caisse.dto.AbonnementDTO;
import caisse.manager.caisse.dto.UpdateManagerRequest;
import caisse.manager.caisse.dto.UtilisateurSuperAdminDTO;
import caisse.manager.caisse.model.Snack;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.model.Plan;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.IngredientRepository;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/super-admin")
// Sécurité : Seul le SUPER_ADMIN peut accéder à ces méthodes
// (Nécessite d'activer @EnableMethodSecurity dans SecurityConfig, ou de le faire via requestMatchers)
@CrossOrigin("*") // Décommenter si vous avez des soucis CORS en local malgré la config

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

    // 1. LISTER TOUS LES RESTAURANTS
    @GetMapping("/snacks")
    public List<Snack> getAllSnacks() {
        return snackRepository.findAll();
    }

    // 2. CRÉER UN NOUVEAU RESTAURANT + SON MANAGER
    @PostMapping("/snacks")
    @Transactional // Important : Si la création du user échoue, le snack n'est pas créé (Rollback)
    public ResponseEntity<?> createSnack(@RequestBody CreateSnackRequest request) {

        // A. Créer le Snack
        Snack snack = new Snack();
        snack.setNom(request.getNomRestaurant());
        snack.setAdresse(request.getAdresse());
        snack = snackRepository.save(snack); // On récupère l'objet avec son nouvel ID

        // B. Créer l'utilisateur Manager lié à ce Snack
        Utilisateur manager = new Utilisateur();
        manager.setUsername(request.getUsernameManager());
        manager.setPassword(passwordEncoder.encode(request.getPasswordManager()));
        manager.setRole("MANAGER");
        manager.setSnackId(snack.getId()); // LIEN CLÉ : On lie le user au nouveau snack

        utilisateurRepository.save(manager);

        return ResponseEntity.ok("Restaurant '" + snack.getNom() + "' créé avec succès (ID: " + snack.getId() + ")");
    }

    // 3. MODIFIER UN SNACK (PUT)
    @PutMapping("/snacks/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateSnack(@PathVariable Long id, @RequestBody SnackUpdateRequest request) {
        return snackRepository.findById(id).map(snack -> {
            if (request.getNom() != null && !request.getNom().trim().isEmpty()) {
                snack.setNom(request.getNom());
            }
            if (request.getAdresse() != null) {
                snack.setAdresse(request.getAdresse());
            }
            snackRepository.save(snack);
            return ResponseEntity.ok(snack); // Retourner le snack modifié
        }).orElse(ResponseEntity.notFound().build());
    }
    
    // 3.1. MODIFIER LE MANAGER D'UN SNACK
    @PutMapping("/snacks/{id}/manager")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateManager(@PathVariable Long id, @RequestBody UpdateManagerRequest request) {
        // Trouver le snack
        Snack snack = snackRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Snack non trouvé"));
        
        // Trouver le manager du snack
        Optional<Utilisateur> managerOpt = utilisateurRepository.findBySnackIdAndRole(snack.getId(), "MANAGER");
        if (managerOpt.isEmpty()) {
            return ResponseEntity.status(404).body("❌ Aucun manager trouvé pour ce restaurant.");
        }
        
        Utilisateur manager = managerOpt.get();
        
        // Modifier username si fourni
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            // Vérifier unicité
            Optional<Utilisateur> existingUser = utilisateurRepository.findByUsername(request.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(manager.getId())) {
                return ResponseEntity.badRequest().body("❌ Ce nom d'utilisateur existe déjà.");
            }
            manager.setUsername(request.getUsername());
        }
        
        // Modifier password si fourni
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            manager.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        utilisateurRepository.save(manager);
        return ResponseEntity.ok("✅ Manager modifié avec succès.");
    }

    // 4. ACTIVER / DÉSACTIVER UN SNACK (PUT)
    @PutMapping("/snacks/{id}/status")
    public ResponseEntity<?> toggleSnackStatus(@PathVariable Long id) {
        return snackRepository.findById(id).map(snack -> {
            boolean nouveauStatut = !snack.isActif(); // On inverse (Vrai -> Faux, Faux -> Vrai)
            snack.setActif(nouveauStatut);
            snackRepository.save(snack);

            String message = nouveauStatut ? "✅ Restaurant réactivé." : "⛔ Restaurant suspendu.";
            return ResponseEntity.ok(message);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. SUPPRIMER UN SNACK (DELETE) - Suppression en cascade
    @DeleteMapping("/snacks/{id}")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteSnack(@PathVariable Long id) {
        if (!snackRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // 1. Supprimer tous les utilisateurs liés à ce snack
        utilisateurRepository.deleteBySnackId(id);

        // 2. Supprimer tous les produits liés à ce snack
        produitRepository.findBySnackId(id).forEach(produit -> produitRepository.delete(produit));

        // 3. Supprimer tous les ingrédients liés à ce snack
        ingredientRepository.findBySnackId(id).forEach(ingredient -> ingredientRepository.delete(ingredient));

        // 4. Supprimer toutes les commandes liées à ce snack
        commandeRepository.findBySnackIdOrderByDateDesc(id).forEach(commande -> commandeRepository.delete(commande));

        // 5. Supprimer le snack lui-même
        snackRepository.deleteById(id);

        return ResponseEntity.ok("🗑️ Restaurant et toutes ses données supprimés définitivement.");
    }

    // 6. RÉINITIALISER LE MOT DE PASSE DU MANAGER
    @PostMapping("/snacks/{snackId}/reset-manager-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> resetManagerPassword(@PathVariable Long snackId) {
        // Trouver le manager du snack
        Optional<Utilisateur> managerOpt = utilisateurRepository.findBySnackIdAndRole(snackId, "MANAGER");
        
        if (managerOpt.isEmpty()) {
            return ResponseEntity.status(404).body("❌ Aucun manager trouvé pour ce restaurant.");
        }

        Utilisateur manager = managerOpt.get();

        // Générer un mot de passe temporaire sécurisé (8 caractères aléatoires)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder tempPassword = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            tempPassword.append(chars.charAt(random.nextInt(chars.length())));
        }
        String tempPasswordStr = tempPassword.toString();

        // Hasher le mot de passe
        String hashedPassword = passwordEncoder.encode(tempPasswordStr);
        manager.setPassword(hashedPassword);
        utilisateurRepository.save(manager);

        // Renvoyer le mot de passe temporaire en clair
        return ResponseEntity.ok(tempPasswordStr);
    }

    // 7. GÉRER L'ABONNEMENT D'UN SNACK
    @PutMapping("/snacks/{id}/abonnement")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> gererAbonnement(@PathVariable Long id, @RequestBody AbonnementDTO dto) {
        return snackRepository.findById(id).map(snack -> {
            if (dto.getPlanId() != null) {
                Plan plan = planRepository.findById(dto.getPlanId())
                        .orElseThrow(() -> new RuntimeException("Plan introuvable"));
                snack.setPlan(plan);
            }
            if (dto.getDateFinAbonnement() != null && !dto.getDateFinAbonnement().isEmpty()) {
                snack.setDateFinAbonnement(LocalDate.parse(dto.getDateFinAbonnement()));
            }
            snackRepository.save(snack);
            return ResponseEntity.ok("✅ Abonnement mis à jour avec succès.");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 8. LISTER TOUS LES UTILISATEURS (COMPTES ENREGISTRÉS)
    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UtilisateurSuperAdminDTO>> getAllUsers() {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        
        List<UtilisateurSuperAdminDTO> dtos = utilisateurs.stream()
            .map(u -> {
                UtilisateurSuperAdminDTO dto = new UtilisateurSuperAdminDTO();
                dto.setId(u.getId());
                dto.setUsername(u.getUsername());
                dto.setRole(u.getRole());
                dto.setActif(u.getActif() != null ? u.getActif() : true);
                dto.setSnackId(u.getSnackId());
                dto.setDateCreation(u.getDateCreation());
                
                // Récupérer le nom du snack si snackId existe
                if (u.getSnackId() != null) {
                    snackRepository.findById(u.getSnackId())
                        .ifPresent(snack -> dto.setSnackNom(snack.getNom()));
                } else {
                    dto.setSnackNom("Super Admin"); // Pour les super admins qui n'ont pas de snack
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    // 9. SUPPRIMER UN UTILISATEUR
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(id);
        
        if (utilisateurOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Utilisateur utilisateur = utilisateurOpt.get();
        
        // Empêcher la suppression du super admin principal
        if ("SUPER_ADMIN".equals(utilisateur.getRole()) || "ROLE_SUPER_ADMIN".equals(utilisateur.getRole())) {
            // Vérifier s'il y a d'autres super admins
            long superAdminCount = utilisateurRepository.findAll().stream()
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()) || "ROLE_SUPER_ADMIN".equals(u.getRole()))
                .count();
            
            if (superAdminCount <= 1) {
                return ResponseEntity.badRequest().body("❌ Impossible de supprimer le dernier Super Admin.");
            }
        }
        
        utilisateurRepository.delete(utilisateur);
        return ResponseEntity.ok("✅ Utilisateur supprimé avec succès.");
    }
}