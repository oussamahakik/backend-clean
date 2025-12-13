package caisse.manager.caisse.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lignes_commande")
@Data
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomProduit;

    private Double prixUnitaire;

    private Integer quantite;

    @Column(length = 1000)
    private String details; // "Sauce blanche, Sans oignon..."

    // Lien vers la commande parente
    @ManyToOne
    @JoinColumn(name = "commande_id")
    @JsonBackReference
    private Commande commande;
}