package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Snack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SnackRepository extends JpaRepository<Snack, Long> {
    Optional<Snack> findByKioskSlug(String kioskSlug);
    boolean existsByKioskSlugAndIdNot(String kioskSlug, Long id);
}
