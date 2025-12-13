package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class LigneCommandeRequest {
    private Long produitId;
    private int quantite;
    private String details;

    private Double prixFinal;
}