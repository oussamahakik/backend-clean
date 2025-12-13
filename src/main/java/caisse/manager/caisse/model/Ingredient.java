package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ingredients")
@Data
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snack_id")
    private Long snackId;

    private String nom;

    @Enumerated(EnumType.STRING)
    private TypeIngredient type; // VIANDE, SAUCE, SUPPLEMENT

    private Double prixSupplement; // 0.0 si gratuit

    private Boolean disponible = true;
}