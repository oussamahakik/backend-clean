package caisse.manager.caisse.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VenteDetailDTO {
    private String nomProduit;
    private Integer quantite;
    private Double prixUnitaire;
    private Double prixTotal;
    private LocalDateTime dateVente;
    private String typePaiement;
    private Long commandeId;
}












