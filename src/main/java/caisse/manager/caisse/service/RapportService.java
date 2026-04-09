package caisse.manager.caisse.service;

import caisse.manager.caisse.dto.*;
import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.LigneCommande;
import caisse.manager.caisse.repository.CommandeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RapportService {

    @Autowired
    private CommandeRepository commandeRepository;

    public RapportDTO getStatistiques(Long snackId, String periode) {
        LocalDateTime dateDebut = getDateDebut(periode);
        List<Commande> commandes = commandeRepository.findBySnackIdOrderByDateDesc(snackId)
                .stream()
                .filter(c -> c.getDate() != null && c.getDate().isAfter(dateDebut))
                .collect(Collectors.toList());

        StatsGlobalesDTO statsGlobales = calculerStatsGlobales(commandes, snackId);
        
        // NOUVEAU : Détails des ventes du jour (seulement si période = JOUR)
        List<VenteDetailDTO> ventesDetail = null;
        if ("JOUR".equals(periode.toUpperCase())) {
            ventesDetail = calculerVentesDetail(snackId);
        }
        
        List<TopProduitDTO> topProduits = calculerTopProduits(commandes);
        List<VentesCategorieDTO> ventesParCategorie = calculerVentesParCategorie(commandes);

        RapportDTO rapport = new RapportDTO();
        rapport.setStatsGlobales(statsGlobales);
        rapport.setVentesDetail(ventesDetail);
        rapport.setTopProduits(topProduits);
        rapport.setVentesParCategorie(ventesParCategorie);

        return rapport;
    }
    
    private List<VenteDetailDTO> calculerVentesDetail(Long snackId) {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime startOfDay = aujourdhui.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<Commande> commandesDuJour = commandeRepository.findBySnackIdAndDateBetweenOrderByDateDesc(
                snackId,
                startOfDay,
                endOfDay
        );
        
        return commandesDuJour.stream()
            .flatMap(commande -> commande.getLignes().stream()
                .map(ligne -> {
                    VenteDetailDTO detail = new VenteDetailDTO();
                    detail.setNomProduit(ligne.getNomProduit());
                    detail.setQuantite(ligne.getQuantite());
                    detail.setPrixUnitaire(ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : 0.0);
                    detail.setPrixTotal((ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : 0.0) * ligne.getQuantite());
                    detail.setDateVente(commande.getDate());
                    detail.setTypePaiement(commande.getTypePaiement());
                    detail.setCommandeId(commande.getId());
                    return detail;
                })
            )
            .collect(Collectors.toList());
    }

    private LocalDateTime getDateDebut(String periode) {
        LocalDate aujourdhui = LocalDate.now();
        switch (periode.toUpperCase()) {
            case "JOUR":
                return aujourdhui.atStartOfDay();
            case "SEMAINE":
                return aujourdhui.minusDays(7).atStartOfDay();
            case "MOIS":
                return aujourdhui.minusMonths(1).atStartOfDay();
            default:
                return LocalDateTime.of(2000, 1, 1, 0, 0); // Toutes les données
        }
    }

    private StatsGlobalesDTO calculerStatsGlobales(List<Commande> commandes, Long snackId) {
        StatsGlobalesDTO stats = new StatsGlobalesDTO();

        // CA total
        double caTotal = commandes.stream()
                .filter(c -> c.getTotal() != null)
                .mapToDouble(Commande::getTotal)
                .sum();
        stats.setChiffreAffaires(caTotal);

        // Total commandes
        stats.setTotalCommandes((long) commandes.size());

        // Panier moyen
        if (!commandes.isEmpty()) {
            stats.setPanierMoyen(caTotal / commandes.size());
        } else {
            stats.setPanierMoyen(0.0);
        }

        // Commandes aujourd'hui
        LocalDate aujourdhui = LocalDate.now();
        long commandesAujourdhui = commandes.stream()
                .filter(c -> c.getDate() != null && c.getDate().toLocalDate().equals(aujourdhui))
                .count();
        stats.setCommandesAujourdhui(commandesAujourdhui);

        // CA aujourd'hui
        double caAujourdhui = commandes.stream()
                .filter(c -> c.getDate() != null && c.getDate().toLocalDate().equals(aujourdhui))
                .filter(c -> c.getTotal() != null)
                .mapToDouble(Commande::getTotal)
                .sum();
        stats.setCaAujourdhui(caAujourdhui);

        return stats;
    }

    private List<TopProduitDTO> calculerTopProduits(List<Commande> commandes) {
        Map<String, TopProduitDTO> produitsMap = new HashMap<>();

        for (Commande commande : commandes) {
            if (commande.getLignes() != null) {
                for (LigneCommande ligne : commande.getLignes()) {
                    String nomProduit = ligne.getNomProduit();
                    TopProduitDTO produit = produitsMap.getOrDefault(nomProduit, new TopProduitDTO());
                    produit.setNomProduit(nomProduit);
                    produit.setQuantiteVendue(produit.getQuantiteVendue() == null ? 0L : produit.getQuantiteVendue() + ligne.getQuantite());
                    double ca = (ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : 0.0) * ligne.getQuantite();
                    produit.setChiffreAffaires(produit.getChiffreAffaires() == null ? 0.0 : produit.getChiffreAffaires() + ca);
                    produitsMap.put(nomProduit, produit);
                }
            }
        }

        return produitsMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getQuantiteVendue(), a.getQuantiteVendue()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<VentesCategorieDTO> calculerVentesParCategorie(List<Commande> commandes) {
        // Pour calculer par catégorie, on doit récupérer la catégorie depuis le produit
        // Comme on n'a pas la catégorie dans LigneCommande, on va utiliser une approche simplifiée
        // En analysant le nom du produit ou en utilisant une logique de mapping
        
        Map<String, VentesCategorieDTO> categoriesMap = new HashMap<>();
        
        // Mapping simplifié basé sur le nom du produit (à améliorer avec une vraie relation)
        for (Commande commande : commandes) {
            if (commande.getLignes() != null) {
                for (LigneCommande ligne : commande.getLignes()) {
                    String categorie = determinerCategorie(ligne.getNomProduit());
                    VentesCategorieDTO cat = categoriesMap.getOrDefault(categorie, new VentesCategorieDTO());
                    cat.setCategorie(categorie);
                    double ca = (ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : 0.0) * ligne.getQuantite();
                    cat.setChiffreAffaires(cat.getChiffreAffaires() == null ? 0.0 : cat.getChiffreAffaires() + ca);
                    cat.setNombreVentes(cat.getNombreVentes() == null ? 0L : cat.getNombreVentes() + ligne.getQuantite());
                    categoriesMap.put(categorie, cat);
                }
            }
        }

        return new ArrayList<>(categoriesMap.values());
    }

    private String determinerCategorie(String nomProduit) {
        String nom = nomProduit.toUpperCase();
        if (nom.contains("TACOS") || nom.contains("TACO")) return "Tacos";
        if (nom.contains("BURGER")) return "Burgers";
        if (nom.contains("PIZZA")) return "Pizzas";
        if (nom.contains("COCA") || nom.contains("EAU") || nom.contains("BOISSON")) return "Boissons";
        if (nom.contains("FRITE") || nom.contains("NUGGET")) return "Accompagnements";
        return "Autres";
    }
}
