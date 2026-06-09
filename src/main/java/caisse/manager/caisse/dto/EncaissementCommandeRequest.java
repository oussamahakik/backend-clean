package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class EncaissementCommandeRequest {
    private String typePaiement; // ESPECES ou CARTE
}
