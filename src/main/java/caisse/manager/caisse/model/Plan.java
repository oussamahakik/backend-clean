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

    @Column(name = "prixMensuel", nullable = false)
    private Double prixMensuel;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "nombre_restaurants_max")
    private Integer nombreRestaurantsMax; // null = illimité
    
    @Column(name = "nombre_utilisateurs_max")
    private Integer nombreUtilisateursMax; // null = illimité
    
    @Column(nullable = false)
    private Boolean actif = true;
}

