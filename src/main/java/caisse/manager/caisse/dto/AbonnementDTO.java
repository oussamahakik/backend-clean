package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class AbonnementDTO {
    private Long planId;
    private String dateFinAbonnement; // Format: "YYYY-MM-DD"
}

