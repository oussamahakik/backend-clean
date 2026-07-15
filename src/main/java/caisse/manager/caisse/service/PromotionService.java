package caisse.manager.caisse.service;

import caisse.manager.caisse.model.Promotion;
import caisse.manager.caisse.model.Produit;
import caisse.manager.caisse.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer l'application automatique des promotions
 */
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    /**
     * Trouve la promotion active applicable à un produit selon sa catégorie
     * Si plusieurs promotions existent, retourne la plus avantageuse pour le client
     * @param produit Le produit à vérifier
     * @param snackId L'ID du snack
     * @param prixInitial Le prix initial pour calculer quelle promotion est la meilleure
     * @return La promotion active la plus avantageuse, ou null si aucune
     */
    public Promotion trouverMeilleurePromotionPourProduit(Produit produit, Long snackId, double prixInitial) {
        if (produit == null || produit.getCategorie() == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        List<Promotion> promotions = promotionRepository
                .findByCategorieAndSnackIdAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        produit.getCategorie(), snackId, today, today);

        if (promotions.isEmpty()) {
            return null;
        }

        return trouverMeilleurePromotion(promotions, prixInitial);
    }

    /**
     * Trouve la promotion active pour un produit spécifique (par produitId)
     * @param produitId L'ID du produit
     * @param snackId L'ID du snack
     * @return La promotion active applicable, ou null si aucune
     */
    public Promotion trouverPromotionActivePourProduitId(Long produitId, Long snackId) {
        if (produitId == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        List<Promotion> promotions = promotionRepository
                .findByProduitIdAndSnackIdAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        produitId, snackId, today, today);

        if (promotions.isEmpty()) {
            return null;
        }

        return promotions.get(0);
    }

    /**
     * Calcule le prix après application d'une promotion
     * @param prixInitial Le prix initial du produit
     * @param promotion La promotion à appliquer
     * @return Le prix réduit
     */
    public double calculerPrixAvecPromotion(double prixInitial, Promotion promotion) {
        if (promotion == null) {
            return prixInitial;
        }

        String typePromotion = promotion.getTypePromotion();
        Double valeur = promotion.getValeur();

        if (valeur == null || valeur <= 0) {
            return prixInitial;
        }

        switch (typePromotion) {
            case "POURCENTAGE":
                return Math.max(0.0, prixInitial * (1 - valeur / 100.0));

            case "MONTANT_FIXE":
            case "MONTANT":
                return Math.max(0.0, prixInitial - valeur);

            case "CODE_PROMO":
                double reductionCode = prixInitial * (valeur / 100.0);
                return Math.max(0.0, prixInitial - reductionCode);

            default:
                return prixInitial;
        }
    }

    /**
     * Trouve la meilleure promotion parmi une liste de promotions
     * Compare les prix finaux après application de chaque promotion
     * @param promotions Liste de promotions à comparer
     * @param prixInitial Prix initial pour le calcul
     * @return La promotion qui donne le meilleur prix (le plus bas)
     */
    private Promotion trouverMeilleurePromotion(List<Promotion> promotions, double prixInitial) {
        if (promotions.isEmpty()) {
            return null;
        }

        Promotion meilleurePromotion = null;
        double meilleurPrix = prixInitial;

        for (Promotion promo : promotions) {
            double prixAvecPromo = calculerPrixAvecPromotion(prixInitial, promo);
            if (prixAvecPromo < meilleurPrix) {
                meilleurPrix = prixAvecPromo;
                meilleurePromotion = promo;
            }
        }

        return meilleurePromotion;
    }

    /**
     * Trouve et applique automatiquement la meilleure promotion pour un produit
     * Priorité : Promotion sur produit spécifique > Promotion sur catégorie
     * Si plusieurs promotions existent au même niveau, choisit la plus avantageuse
     * 
     * @param produit Le produit
     * @param snackId L'ID du snack
     * @param prixInitial Le prix initial (peut venir du Frontend avec suppléments)
     * @return Le prix après application de la promotion (ou prixInitial si aucune promotion)
     */
    public double appliquerPromotionAutomatique(Produit produit, Long snackId, double prixInitial) {
        if (produit == null) {
            return prixInitial;
        }

        LocalDate today = LocalDate.now();
        List<Promotion> promotionsProduit = promotionRepository
                .findByProduitIdAndSnackIdAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        produit.getId(), snackId, today, today);

        if (!promotionsProduit.isEmpty()) {
            Promotion meilleurePromoProduit = trouverMeilleurePromotion(promotionsProduit, prixInitial);
            if (meilleurePromoProduit != null) {
                return calculerPrixAvecPromotion(prixInitial, meilleurePromoProduit);
            }
        }

        Promotion meilleurePromoCategorie = trouverMeilleurePromotionPourProduit(produit, snackId, prixInitial);
        if (meilleurePromoCategorie != null) {
            return calculerPrixAvecPromotion(prixInitial, meilleurePromoCategorie);
        }

        return prixInitial;
    }
}
