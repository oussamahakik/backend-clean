package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class StatsGlobalesDTO {
    private Double chiffreAffaires;
    private Long totalCommandes;
    private Double panierMoyen;
    private Long commandesAujourdhui;
    private Double caAujourdhui;
}

