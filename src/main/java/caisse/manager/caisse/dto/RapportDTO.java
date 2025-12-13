package caisse.manager.caisse.dto;

import lombok.Data;
import java.util.List;

@Data
public class RapportDTO {
    private StatsGlobalesDTO statsGlobales;
    private List<VenteDetailDTO> ventesDetail; // NOUVEAU : Détails des ventes du jour
    private List<TopProduitDTO> topProduits;
    private List<VentesCategorieDTO> ventesParCategorie;
}

