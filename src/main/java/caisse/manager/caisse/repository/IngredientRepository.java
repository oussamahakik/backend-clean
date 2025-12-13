package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    // Trouver tous les ingrédients d'un snack précis
    List<Ingredient> findBySnackId(Long snackId);
}