package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.IntegrityCheckResultDTO;
import caisse.manager.caisse.dto.StatisticsDTO;
import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.Ingredient;
import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.model.Snack;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.CommandeRepository;
import caisse.manager.caisse.repository.IngredientRepository;
import caisse.manager.caisse.repository.ProduitRepository;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostic")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class DiagnosticController {

    private final SnackRepository snackRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CommandeRepository commandeRepository;
    private final IngredientRepository ingredientRepository;

    /**
     * Vérification complète de l'intégrité référentielle de la base de données
     */
    @GetMapping("/integrity-check")
    @Transactional(readOnly = true)
    public ResponseEntity<IntegrityReportDTO> checkIntegrity() {
        log.info("Démarrage de la vérification d'intégrité de la base de données");
        
        IntegrityReportDTO report = new IntegrityReportDTO();
        List<IntegrityCheckResultDTO> issues = new ArrayList<>();
        
        // Récupérer tous les snacks
        List<Snack> allSnacks = snackRepository.findAll();
        List<Long> validSnackIds = allSnacks.stream()
            .map(Snack::getId)
            .collect(Collectors.toList());
        
        // 1. Vérifier les produits orphelins
        log.debug("Vérification des produits orphelins...");
        List<Produit> allProduits = produitRepository.findAll();
        for (Produit produit : allProduits) {
            if (produit.getSnackId() != null && !validSnackIds.contains(produit.getSnackId())) {
                issues.add(new IntegrityCheckResultDTO(
                    "PRODUITS",
                    "Produit avec snack_id invalide",
                    produit.getSnackId(),
                    1L,
                    true
                ));
            }
        }
        
        // Compter les produits orphelins
        long produitsOrphelins = allProduits.stream()
            .filter(p -> p.getSnackId() != null && !validSnackIds.contains(p.getSnackId()))
            .count();
        
        if (produitsOrphelins > 0) {
            report.getSummary().put("produits_orphelins", produitsOrphelins);
        }
        
        // 2. Vérifier les utilisateurs orphelins
        log.debug("Vérification des utilisateurs orphelins...");
        List<Utilisateur> allUtilisateurs = utilisateurRepository.findAll();
        for (Utilisateur utilisateur : allUtilisateurs) {
            if (utilisateur.getSnackId() != null && !validSnackIds.contains(utilisateur.getSnackId())) {
                issues.add(new IntegrityCheckResultDTO(
                    "UTILISATEURS",
                    "Utilisateur avec snack_id invalide",
                    utilisateur.getSnackId(),
                    1L,
                    true
                ));
            }
        }
        
        long utilisateursOrphelins = allUtilisateurs.stream()
            .filter(u -> u.getSnackId() != null && !validSnackIds.contains(u.getSnackId()))
            .count();
        
        if (utilisateursOrphelins > 0) {
            report.getSummary().put("utilisateurs_orphelins", utilisateursOrphelins);
        }
        
        // 3. Vérifier les commandes orphelines
        log.debug("Vérification des commandes orphelines...");
        List<Commande> allCommandes = commandeRepository.findAll();
        for (Commande commande : allCommandes) {
            if (commande.getSnackId() != null && !validSnackIds.contains(commande.getSnackId())) {
                issues.add(new IntegrityCheckResultDTO(
                    "COMMANDES",
                    "Commande avec snack_id invalide",
                    commande.getSnackId(),
                    1L,
                    true
                ));
            }
        }
        
        long commandesOrphelines = allCommandes.stream()
            .filter(c -> c.getSnackId() != null && !validSnackIds.contains(c.getSnackId()))
            .count();
        
        if (commandesOrphelines > 0) {
            report.getSummary().put("commandes_orphelines", commandesOrphelines);
        }
        
        // 4. Vérifier les ingrédients orphelins
        log.debug("Vérification des ingrédients orphelins...");
        List<Ingredient> allIngredients = ingredientRepository.findAll();
        for (Ingredient ingredient : allIngredients) {
            if (ingredient.getSnackId() != null && !validSnackIds.contains(ingredient.getSnackId())) {
                issues.add(new IntegrityCheckResultDTO(
                    "INGREDIENTS",
                    "Ingrédient avec snack_id invalide",
                    ingredient.getSnackId(),
                    1L,
                    true
                ));
            }
        }
        
        long ingredientsOrphelins = allIngredients.stream()
            .filter(i -> i.getSnackId() != null && !validSnackIds.contains(i.getSnackId()))
            .count();
        
        if (ingredientsOrphelins > 0) {
            report.getSummary().put("ingredients_orphelins", ingredientsOrphelins);
        }
        
        // Regrouper les résultats pour le rapport dans le summary
        Map<String, Long> groupedIssues = issues.stream()
            .collect(Collectors.groupingBy(
                IntegrityCheckResultDTO::getTableName,
                Collectors.summingLong(IntegrityCheckResultDTO::getCount)
            ));
        
        report.getSummary().putAll(groupedIssues);
        report.setIssues(issues);
        report.setTotalIssues((long) issues.size());
        report.setHasIssues(!issues.isEmpty());
        
        log.info("Vérification terminée. {} problème(s) détecté(s).", issues.size());
        
        return ResponseEntity.ok(report);
    }

    /**
     * Récupération des statistiques par snack
     */
    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StatisticsDTO>> getStatistics() {
        log.info("Récupération des statistiques par snack");
        
        List<Snack> allSnacks = snackRepository.findAll();
        List<StatisticsDTO> statistics = new ArrayList<>();
        
        for (Snack snack : allSnacks) {
            Long snackId = snack.getId();
            
            StatisticsDTO stats = new StatisticsDTO();
            stats.setSnackId(snackId);
            stats.setSnackNom(snack.getNom() != null ? snack.getNom() : "Sans nom");
            
            // Compter les produits
            long nombreProduits = produitRepository.findAll().stream()
                .filter(p -> snackId.equals(p.getSnackId()))
                .count();
            stats.setNombreProduits(nombreProduits);
            
            // Compter les utilisateurs
            long nombreUtilisateurs = utilisateurRepository.findAll().stream()
                .filter(u -> snackId.equals(u.getSnackId()))
                .count();
            stats.setNombreUtilisateurs(nombreUtilisateurs);
            
            // Compter les commandes
            long nombreCommandes = commandeRepository.findAll().stream()
                .filter(c -> snackId.equals(c.getSnackId()))
                .count();
            stats.setNombreCommandes(nombreCommandes);
            
            statistics.add(stats);
        }
        
        log.info("Statistiques récupérées pour {} snack(s)", statistics.size());
        return ResponseEntity.ok(statistics);
    }

    /**
     * DTO pour le rapport d'intégrité
     */
    @Data
    public static class IntegrityReportDTO {
        private List<IntegrityCheckResultDTO> issues = new ArrayList<>();
        private Map<String, Long> summary = new HashMap<>();
        private Long totalIssues = 0L;
        private boolean hasIssues = false;
    }
}

