package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateurs")
@Data
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // Ex: "admin_tacos"

    @Column(nullable = false)
    private String password; // Sera crypté plus tard

    // C'est ICI que se fait le lien avec le snack (conservé pour compatibilité avec le code existant)
    @Column(name = "snack_id", nullable = true)
    private Long snackId;
    
    // Relation JPA vers Snack (désactivée pour éviter les problèmes de FK)
    // Note: Cette relation n'est pas utilisée dans le code existant qui utilise directement snackId
    // Désactivée car elle cause des problèmes de contraintes FK mal formées
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "snack_id", nullable = true, insertable = false, updatable = false)
    // @JsonIgnore
    // private Snack snack;

    private String role; // Ex: "ROLE_MANAGER", "ROLE_CAISSIER"
    
    private Boolean actif = true; // Par défaut actif
    
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;
}