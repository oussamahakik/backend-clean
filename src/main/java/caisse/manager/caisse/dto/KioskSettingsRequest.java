package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class KioskSettingsRequest {
    private Boolean enabled;
    private String slug;
    private String pin;
}
