package caisse.manager.caisse.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PromotionDTO {
    private Long id;
    private String nom;
    private String description;
    private String typePromotion; // POURCENTAGE, MONTANT_FIXE, CODE_PROMO
    private Double valeur;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String codePromo;
    private Boolean actif;
    private Long produitId;
    private String categorie;
    private Long snackId;
    private Integer nombreUtilisations;
    private Integer nombreUtilisationsMax;
}


