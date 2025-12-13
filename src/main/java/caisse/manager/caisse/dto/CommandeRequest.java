package caisse.manager.caisse.dto;

import lombok.Data;
import java.util.List;

@Data
public class CommandeRequest {
    private List<LigneCommandeRequest> articles;
    private String typePaiement; // "ESPECES" ou "CARTE" (sans accent pour compatibilité)
    private Double remise; // Remise en euros (optionnel)
}