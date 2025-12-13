package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class ImprimanteDTO {
    private Long id;
    private String nom;
    private String type; // USB, RESEAU, BLUETOOTH, FILE
    private String chemin;
    private Long snackId;
    private Boolean actif;
    private String description;
    private Integer largeurPapier;
    private Integer copies;
    private Boolean impressionAuto;
    private String typeTicket; // COMMANDE, RECU, CUISINE
}


