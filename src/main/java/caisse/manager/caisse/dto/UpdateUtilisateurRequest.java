package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class UpdateUtilisateurRequest {
    private String username;
    private String password; // Optionnel
    private String role;
}












