package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "imprimantes")
@Data
public class Imprimante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nom; // Ex: "Imprimante Cuisine", "Imprimante Caisse"
    
    @Column(nullable = false)
    private String type; // USB, RESEAU, BLUETOOTH, FILE
    
    @Column(nullable = false)
    private String chemin; // Nom de l'imprimante système, IP, ou chemin fichier
    
    @Column(nullable = false)
    private Long snackId; // Restaurant concerné
    
    @Column(nullable = false)
    private Boolean actif = true;
    
    @Column(length = 500)
    private String description; // Description de l'utilisation
    
    // Paramètres d'impression
    @Column(name = "largeur_papier")
    private Integer largeurPapier = 80; // mm
    
    @Column(name = "copies")
    private Integer copies = 1;
    
    @Column(name = "impression_auto")
    private Boolean impressionAuto = false;
    
    @Column(name = "type_ticket")
    private String typeTicket = "COMMANDE"; // COMMANDE, RECU, CUISINE
    
    private LocalDateTime dateCreation = LocalDateTime.now();
    
    private LocalDateTime dateModification = LocalDateTime.now();
}


