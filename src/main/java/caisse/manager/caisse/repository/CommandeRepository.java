package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findBySnackIdOrderByDateDesc(Long snackId);

    // Trouver par statut (ex: pour l'écran cuisine)
    List<Commande> findBySnackIdAndStatut(Long snackId, StatutCommande statut);

    // Trouver par plusieurs statuts (pour l'historique)
    List<Commande> findBySnackIdAndStatutInOrderByDateDesc(Long snackId, List<StatutCommande> statuts);
    
    // NOUVELLES MÉTHODES
    List<Commande> findBySnackIdAndDateBetweenOrderByDateDesc(
            Long snackId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );
    
    List<Commande> findBySnackIdAndStatutInAndDateBetweenOrderByDateDesc(
            Long snackId,
            List<StatutCommande> statuts,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    List<Commande> findBySnackIdAndTypePaiementAndStatutOrderByDateAsc(
            Long snackId,
            String typePaiement,
            StatutCommande statut
    );
    
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Commande c WHERE c.snackId = :snackId")
    Double sumChiffreAffairesBySnackId(@Param("snackId") Long snackId);
    
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Commande c WHERE c.snackId = :snackId AND c.date >= :startOfDay AND c.date < :endOfDay")
    Double sumChiffreAffairesBySnackIdAndDate(
            @Param("snackId") Long snackId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
    
    @Query("SELECT COUNT(c) FROM Commande c WHERE c.snackId = :snackId")
    Long countBySnackId(@Param("snackId") Long snackId);
    
    @Query("SELECT COUNT(c) FROM Commande c WHERE c.snackId = :snackId AND c.date >= :startOfDay AND c.date < :endOfDay")
    Long countBySnackIdAndDate(
            @Param("snackId") Long snackId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
