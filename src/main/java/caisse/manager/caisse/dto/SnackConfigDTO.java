package caisse.manager.caisse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SnackConfigDTO {
    // Identité
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    
    // Préférences
    private String themeColor;
    private Boolean notifications;
    private Boolean printAuto;
    private String currency;
}








