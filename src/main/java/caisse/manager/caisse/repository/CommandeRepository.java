package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Commande;
import caisse.manager.caisse.model.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findBySnackIdOrderByDateDesc(Long snackId);

    // Trouver par statut (ex: pour l'écran cuisine)
    List<Commande> findBySnackIdAndStatut(Long snackId, StatutCommande statut);

    // Trouver par plusieurs statuts (pour l'historique)
    List<Commande> findBySnackIdAndStatutInOrderByDateDesc(Long snackId, List<StatutCommande> statuts);
    
    // NOUVELLES MÉTHODES
    @Query("SELECT c FROM Commande c WHERE c.snackId = :snackId AND DATE(c.date) = :date ORDER BY c.date DESC")
    List<Commande> findBySnackIdAndDate(@Param("snackId") Long snackId, @Param("date") LocalDate date);
    
    @Query("SELECT c FROM Commande c WHERE c.snackId = :snackId " +
           "AND c.statut IN ('PRETE', 'SERVIE') " +
           "AND DATE(c.date) = :date " +
           "ORDER BY c.date DESC")
    List<Commande> findBySnackIdAndStatutAndDate(
        @Param("snackId") Long snackId, 
        @Param("date") LocalDate date
    );
    
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Commande c WHERE c.snackId = :snackId")
    Double sumChiffreAffairesBySnackId(@Param("snackId") Long snackId);
    
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Commande c WHERE c.snackId = :snackId AND DATE(c.date) = :date")
    Double sumChiffreAffairesBySnackIdAndDate(@Param("snackId") Long snackId, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(c) FROM Commande c WHERE c.snackId = :snackId")
    Long countBySnackId(@Param("snackId") Long snackId);
    
    @Query("SELECT COUNT(c) FROM Commande c WHERE c.snackId = :snackId AND DATE(c.date) = :date")
    Long countBySnackIdAndDate(@Param("snackId") Long snackId, @Param("date") LocalDate date);
}