package caisse.manager.caisse.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes")
@Data
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snack_id", nullable = false)
    private Long snackId;

    private LocalDateTime date;

    private Double total;

    private Double remise; // Remise en euros (optionnel)

    private String typePaiement; // "ESPECES", "CARTE" (sans accent pour compatibilité)

    @Enumerated(EnumType.STRING)
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    // Liste des produits achetés
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<LigneCommande> lignes = new ArrayList<>();
}