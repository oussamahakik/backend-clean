package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class SnackUpdateRequest {
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    
    // Préférences
    private String themeColor;
    private Boolean printAuto;
    private String currency;
    private Boolean notifications;
}

