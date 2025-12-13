package caisse.manager.caisse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsDTO {
    private Long snackId;
    private String snackNom;
    private Long nombreProduits;
    private Long nombreUtilisateurs;
    private Long nombreCommandes;
}








