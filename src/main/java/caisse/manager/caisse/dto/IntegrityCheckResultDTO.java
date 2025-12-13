package caisse.manager.caisse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegrityCheckResultDTO {
    private String tableName;
    private String description;
    private Long invalidId;
    private Long count;
    private boolean hasIssues;
}








