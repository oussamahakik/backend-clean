package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class TopProduitDTO {
    private String nomProduit;
    private Long quantiteVendue;
    private Double chiffreAffaires;
}

