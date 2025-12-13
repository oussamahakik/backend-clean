package caisse.manager.caisse.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String username;
    private Long snackId;

    // 👇 CETTE LIGNE EST-ELLE PRÉSENTE ?
    private String role;

    // 👇 LE CONSTRUCTEUR PREND-IL BIEN 4 PARAMÈTRES ?
    public AuthResponse(String token, String username, Long snackId, String role) {
        this.token = token;
        this.username = username;
        this.snackId = snackId;
        this.role = role;
    }
}