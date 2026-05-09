package caisse.manager.caisse.repository;

import caisse.manager.caisse.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {
    
    List<LogEntry> findBySnackIdOrderByTimestampDesc(Long snackId);
    
    List<LogEntry> findByLevelOrderByTimestampDesc(String level);
    
    List<LogEntry> findByCategoryOrderByTimestampDesc(String category);
    
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN ?1 AND ?2 ORDER BY l.timestamp DESC")
    List<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT l FROM LogEntry l WHERE l.snackId = ?1 AND l.timestamp BETWEEN ?2 AND ?3 ORDER BY l.timestamp DESC")
    List<LogEntry> findBySnackIdAndTimestampBetween(Long snackId, LocalDateTime start, LocalDateTime end);
}

