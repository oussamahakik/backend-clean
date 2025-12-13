package caisse.manager.caisse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Une erreur est survenue");
        errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        // Log l'erreur pour le débogage
        e.printStackTrace();
        
        // Détecter les erreurs de base de données courantes
        if (e.getCause() != null) {
            String causeMessage = e.getCause().getMessage();
            if (causeMessage != null) {
                if (causeMessage.contains("Table") && causeMessage.contains("doesn't exist")) {
                    errorResponse.put("message", "Table de base de données manquante. Veuillez exécuter les migrations SQL.");
                    errorResponse.put("suggestion", "Exécutez le script create_missing_tables.sql");
                } else if (causeMessage.contains("Unknown column")) {
                    errorResponse.put("message", "Colonne de base de données manquante. Vérifiez la structure de la table.");
                } else if (causeMessage.contains("Duplicate entry")) {
                    errorResponse.put("message", "Une entrée avec cette valeur existe déjà.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }
        }
        
        // Gérer les violations d'intégrité
        if (e instanceof DataIntegrityViolationException) {
            errorResponse.put("message", "Violation de contrainte de base de données");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        // Gérer les résultats vides
        if (e instanceof EmptyResultDataAccessException) {
            errorResponse.put("message", "Ressource non trouvée");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Argument invalide");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointer(NullPointerException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Erreur de référence nulle");
        errorResponse.put("message", "Une valeur requise est manquante");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}


