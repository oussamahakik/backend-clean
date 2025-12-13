package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
public class Promotion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nom; 
    
    @Column(length = 1000)
    private String description;
    
    // CORRECTION : On force le nom en snake_case pour éviter le bug "Field doesn't have default value"
    @Column(name = "type_promotion", nullable = false)
    private String typePromotion; // POURCENTAGE, MONTANT_FIXE, CODE_PROMO
    
    @Column(nullable = false)
    private Double valeur; 
    
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    
    @Column(name = "code_promo")
    private String codePromo; 
    
    @Column(nullable = false)
    private Boolean actif = true;
    
    @Column(name = "produit_id")
    private Long produitId;
    
    private String categorie; 
    
    @Column(name = "snack_id", nullable = false)
    private Long snackId; 
    
    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();
    
    @Column(name = "nombre_utilisations")
    private Integer nombreUtilisations = 0; 
    
    @Column(name = "nombre_utilisations_max")
    private Integer nombreUtilisationsMax; 
}