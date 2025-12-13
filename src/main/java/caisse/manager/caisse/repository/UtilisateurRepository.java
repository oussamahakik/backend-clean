package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    // Méthode magique pour trouver par pseudo
    Optional<Utilisateur> findByUsername(String username);
    
    // Trouver un utilisateur par snackId et role
    Optional<Utilisateur> findBySnackIdAndRole(Long snackId, String role);
    
    // Supprimer tous les utilisateurs d'un snack
    void deleteBySnackId(Long snackId);
    
    // NOUVELLES MÉTHODES
    List<Utilisateur> findBySnackId(Long snackId);
    
    Optional<Utilisateur> findBySnackIdAndId(Long snackId, Long id);
    
    Optional<Utilisateur> findBySnackIdAndUsername(Long snackId, String username);
    
    boolean existsBySnackIdAndUsername(Long snackId, String username);
}