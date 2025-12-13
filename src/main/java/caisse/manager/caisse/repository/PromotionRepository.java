package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    List<Promotion> findBySnackIdAndActifTrue(Long snackId);
    
    List<Promotion> findBySnackIdOrderByDateCreationDesc(Long snackId);
    
    Optional<Promotion> findByCodePromoAndSnackIdAndActifTrue(String codePromo, Long snackId);
    
    List<Promotion> findByProduitIdAndSnackIdAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
        Long produitId, Long snackId, LocalDate date, LocalDate date2);
    
    List<Promotion> findByCategorieAndSnackIdAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
        String categorie, Long snackId, LocalDate date, LocalDate date2);
    
    /**
     * Méthode optimisée pour trouver toutes les promotions actives pour un snack
     * Utilise une requête JPQL pour éviter les multiples requêtes
     */
    @Query("SELECT p FROM Promotion p WHERE p.snackId = :snackId AND p.actif = true AND :date BETWEEN p.dateDebut AND p.dateFin")
    List<Promotion> findActivePromotions(@Param("snackId") Long snackId, @Param("date") LocalDate date);
}


