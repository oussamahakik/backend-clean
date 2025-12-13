package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "snacks")
@Data
public class Snack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom; // Ex: "Chez Tonton Burger"

    private String adresse;

    private String telephone;
    
    private String email;
    
    private String siteWeb;

    private LocalDate dateCreation = LocalDate.now();

    private boolean actif = true; // Pour bloquer un mauvais payeur ;)

    // Relation vers Plan - nullable pour éviter les problèmes de FK si la table n'existe pas encore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = true)
    private Plan plan;

    private LocalDate dateFinAbonnement;
    
    // Paramètres de configuration du snack
    @Column(name = "theme_color")
    private String themeColor = "light"; // light, dark, auto
    
    @Column(name = "print_auto")
    private Boolean printAuto = false; // Impression automatique des tickets
    
    @Column(name = "currency")
    private String currency = "EUR"; // Devise utilisée
    
    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true; // Activer les notifications
    
    // Relations bidirectionnelles désactivées pour éviter les problèmes de contraintes FK mal formées
    // Ces relations ne sont pas utilisées dans le code existant qui utilise directement snackId
    // @OneToMany(mappedBy = "snack", cascade = {}, fetch = FetchType.LAZY)
    // @JsonIgnore
    // private List<Produit> produits = new ArrayList<>();
    
    // @OneToMany(mappedBy = "snack", cascade = {}, fetch = FetchType.LAZY)
    // @JsonIgnore
    // private List<Utilisateur> utilisateurs = new ArrayList<>();
}