package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class VentesCategorieDTO {
    private String categorie;
    private Double chiffreAffaires;
    private Long nombreVentes;
}

