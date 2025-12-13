package caisse.manager.caisse.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UtilisateurDTO {
    private Long id;
    private String username;
    private String role;
    private Boolean actif;
    private LocalDateTime dateCreation;
}












