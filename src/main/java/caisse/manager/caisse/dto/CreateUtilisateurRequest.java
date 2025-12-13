package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class CreateUtilisateurRequest {
    private String username;
    private String password;
    private String role = "ROLE_CAISSIER"; // Par défaut
}

