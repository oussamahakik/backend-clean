package caisse.manager.caisse.controller;

import caisse.manager.caisse.model.LogEntry;
import caisse.manager.caisse.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
            Specification<LogEntry> spec = buildSpecification(snackId, level, category, startDate, endDate);
            List<LogEntry> logs = logEntryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp"));
            return ResponseEntity.ok(logs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Paramètres invalides", "message", e.getMessage()));
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
            Specification<LogEntry> spec = buildSpecification(snackId, level, category, startDate, endDate);
            List<LogEntry> logs = logEntryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp"));

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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Paramètres invalides", "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur lors de l'export des logs",
                            "message", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    private Specification<LogEntry> buildSpecification(Long snackId, String level, String category, String startDate, String endDate) {
        Specification<LogEntry> spec = Specification.where(null);

        if (snackId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("snackId"), snackId));
        }
        if (level != null && !level.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("level")), level.trim().toUpperCase()));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase()));
        }
        if ((startDate != null && !startDate.isBlank()) || (endDate != null && !endDate.isBlank())) {
            LocalDateTime start = parseDateBoundary(startDate, true);
            LocalDateTime end = parseDateBoundary(endDate, false);
            spec = spec.and((root, query, cb) -> cb.between(root.get("timestamp"), start, end));
        }

        return spec;
    }

    private LocalDateTime parseDateBoundary(String rawValue, boolean startOfDay) {
        if (rawValue == null || rawValue.isBlank()) {
            return startOfDay ? LocalDateTime.MIN : LocalDateTime.MAX;
        }

        String value = rawValue.trim();
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate date = LocalDate.parse(value);
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Format de date invalide. Utilisez YYYY-MM-DD ou YYYY-MM-DDTHH:mm:ss");
            }
        }
    }
}
