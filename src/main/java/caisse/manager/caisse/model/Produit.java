package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "produits")
@Data
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le champ MAGIQUE pour le multi-snack (conservé pour compatibilité avec le code existant)
    @Column(name = "snack_id", nullable = false)
    private Long snackId;
    
    // Relation JPA vers Snack (désactivée pour éviter les problèmes de FK)
    // Note: Cette relation n'est pas utilisée dans le code existant qui utilise directement snackId
    // Désactivée car elle cause des problèmes de contraintes FK mal formées
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "snack_id", nullable = false, insertable = false, updatable = false)
    // @JsonIgnore
    // private Snack snack;

    private String nom;

    private Double prix;

    private String categorie; // Ex: "Boissons", "Burgers"

    private String image; // URL de l'image (optionnel pour l'instant)

    private Boolean disponible; // Pour activer/désactiver un produit sans le supprimer
}