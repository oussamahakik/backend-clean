package caisse.manager.caisse.controller;

import caisse.manager.caisse.model.LogEntry;
import caisse.manager.caisse.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin("*")
public class LogController {

    @Autowired
    private LogEntryRepository logEntryRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllLogs(
            @RequestParam(required = false) Long snackId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<LogEntry> logs;
            
            if (snackId != null) {
                if (startDate != null && endDate != null) {
                    LocalDateTime start = LocalDateTime.parse(startDate);
                    LocalDateTime end = LocalDateTime.parse(endDate);
                    logs = logEntryRepository.findBySnackIdAndTimestampBetween(snackId, start, end);
                } else {
                    logs = logEntryRepository.findBySnackIdOrderByTimestampDesc(snackId);
                }
            } else if (level != null) {
                logs = logEntryRepository.findByLevelOrderByTimestampDesc(level);
            } else if (category != null) {
                logs = logEntryRepository.findByCategoryOrderByTimestampDesc(category);
            } else if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                logs = logEntryRepository.findByTimestampBetween(start, end);
            } else {
                logs = logEntryRepository.findAll();
            }
            
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Erreur lors de la récupération des logs", 
                            "message", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> exportLogs(
            @RequestParam(required = false) Long snackId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<LogEntry> logs;
            
            if (snackId != null) {
                if (startDate != null && endDate != null) {
                    LocalDateTime start = LocalDateTime.parse(startDate);
                    LocalDateTime end = LocalDateTime.parse(endDate);
                    logs = logEntryRepository.findBySnackIdAndTimestampBetween(snackId, start, end);
                } else {
                    logs = logEntryRepository.findBySnackIdOrderByTimestampDesc(snackId);
                }
            } else if (level != null) {
                logs = logEntryRepository.findByLevelOrderByTimestampDesc(level);
            } else if (category != null) {
                logs = logEntryRepository.findByCategoryOrderByTimestampDesc(category);
            } else if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                logs = logEntryRepository.findByTimestampBetween(start, end);
            } else {
                logs = logEntryRepository.findAll();
            }
            
            // Générer le CSV
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Timestamp,Niveau,Catégorie,Message,Username,Snack ID,Détails,IP,User Agent\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (LogEntry log : logs) {
                csv.append(String.format("%d,%s,%s,%s,\"%s\",%s,%s,\"%s\",%s,%s\n",
                    log.getId(),
                    log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "",
                    log.getLevel() != null ? log.getLevel() : "",
                    log.getCategory() != null ? log.getCategory() : "",
                    log.getMessage() != null ? log.getMessage().replace("\"", "\"\"") : "",
                    log.getUsername() != null ? log.getUsername() : "",
                    log.getSnackId() != null ? log.getSnackId() : "",
                    log.getDetails() != null ? log.getDetails().replace("\"", "\"\"") : "",
                    log.getIpAddress() != null ? log.getIpAddress() : "",
                    log.getUserAgent() != null ? log.getUserAgent() : ""
                ));
            }
            
            String filename = "logs_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Erreur lors de l'export des logs", 
                            "message", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }
}

