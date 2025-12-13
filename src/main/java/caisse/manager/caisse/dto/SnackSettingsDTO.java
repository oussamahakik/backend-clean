package caisse.manager.caisse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SnackSettingsDTO {
    private Long id;
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    
    // Préférences
    private String themeColor = "light"; // Par défaut : light
    private Boolean printAuto = false; // Impression automatique
    private String currency = "EUR"; // Devise
    private Boolean notifications = true; // Notifications activées
}



