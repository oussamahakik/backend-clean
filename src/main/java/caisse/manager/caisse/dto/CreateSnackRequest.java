package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class CreateSnackRequest {
    // Infos du Restaurant
    private String nomRestaurant;
    private String adresse;

    // Infos du Gérant (Manager)
    private String usernameManager;
    private String passwordManager;
}