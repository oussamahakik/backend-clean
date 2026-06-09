package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class KioskSettingsResponse {
    private Long snackId;
    private String snackName;
    private Boolean enabled;
    private String slug;
    private Boolean pinConfigured;
    private String kioskUrl;
    private String generatedPin;
}
