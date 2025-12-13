package caisse.manager.caisse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_entries")
@Data
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(nullable = false, length = 50)
    private String level; // INFO, WARN, ERROR, DEBUG
    
    @Column(nullable = false, length = 100)
    private String category; // AUTH, ORDER, PRODUCT, USER, SYSTEM, etc.
    
    @Column(nullable = false, length = 500)
    private String message;
    
    @Column(length = 100)
    private String username; // Utilisateur concerné
    
    private Long snackId; // Restaurant concerné
    
    @Column(length = 1000)
    private String details; // Détails supplémentaires (JSON ou texte)
    
    @Column(length = 50)
    private String ipAddress; // Adresse IP de la requête
    
    @Column(length = 200)
    private String userAgent; // User agent du navigateur
}


