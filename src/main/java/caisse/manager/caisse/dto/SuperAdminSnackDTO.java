package caisse.manager.caisse.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SuperAdminSnackDTO {
    private Long id;
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    private LocalDate dateCreation;
    private boolean actif;
    private LocalDate dateFinAbonnement;
    private PlanDTO plan;
}
