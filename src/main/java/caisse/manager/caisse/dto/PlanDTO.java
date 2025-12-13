package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class PlanDTO {
    private Long id;
    private String nom;
    private Double prixMensuel;
    private String description;
    private Integer nombreRestaurantsMax;
    private Integer nombreUtilisateursMax;
    private Boolean actif = true;
}


