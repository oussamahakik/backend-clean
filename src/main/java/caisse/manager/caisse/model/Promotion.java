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
    private String nom; // Ex: "Réduction été 2024"
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private String typePromotion; // POURCENTAGE, MONTANT_FIXE, CODE_PROMO
    
    @Column(nullable = false)
    private Double valeur; // Pourcentage (ex: 20) ou montant fixe (ex: 5.00)
    
    @Column(nullable = false)
    private LocalDate dateDebut;
    
    @Column(nullable = false)
    private LocalDate dateFin;
    
    private String codePromo; // Pour les promotions avec code
    
    @Column(nullable = false)
    private Boolean actif = true;
    
    // Si null, promotion globale. Sinon, promotion sur un produit spécifique
    private Long produitId;
    
    // Si null, promotion globale. Sinon, promotion sur une catégorie
    private String categorie; // Ex: "BURGER", "TACOS", "BOISSON"
    
    @Column(nullable = false)
    private Long snackId; // Restaurant concerné
    
    private LocalDateTime dateCreation = LocalDateTime.now();
    
    private Integer nombreUtilisations = 0; // Compteur d'utilisations
    
    private Integer nombreUtilisationsMax; // Limite d'utilisations (null = illimité)
}


