package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Spring Data comprend ce nom de méthode et crée la requête SQL automatiquement :
    // "SELECT * FROM produits WHERE snack_id = ?"
    List<Produit> findBySnackId(Long snackId);

    // Pour trouver par catégorie dans un snack précis
    List<Produit> findBySnackIdAndCategorie(Long snackId, String categorie);
}