package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "plans")
@Data
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom; // Ex: "Basic", "Premium", "Enterprise"

    // Nettoyé : Spring va automatiquement créer et mapper vers "prix_mensuel"
    @Column(nullable = false)
    private Double prixMensuel;

    @Column(length = 1000)
    private String description;

    // Nettoyé : Spring va automatiquement créer et mapper vers "nombre_restaurants_max"
    @Column
    private Integer nombreRestaurantsMax; // null = illimité

    // Nettoyé : Spring va automatiquement créer et mapper vers "nombre_utilisateurs_max"
    @Column
    private Integer nombreUtilisateursMax; // null = illimité

    @Column(nullable = false)
    private Boolean actif = true;
}