package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.Imprimante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImprimanteRepository extends JpaRepository<Imprimante, Long> {
    
    List<Imprimante> findBySnackIdOrderByNomAsc(Long snackId);
    
    List<Imprimante> findBySnackIdAndActifTrue(Long snackId);
    
    List<Imprimante> findBySnackIdAndTypeTicket(Long snackId, String typeTicket);
}


