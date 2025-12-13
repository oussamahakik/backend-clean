package caisse.manager.caisse.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UtilisateurSuperAdminDTO {
    private Long id;
    private String username;
    private String role;
    private Boolean actif;
    private Long snackId;
    private String snackNom; // Nom du restaurant associé
    private LocalDateTime dateCreation;
}

