package caisse.manager.caisse.service;

import caisse.manager.caisse.dto.CreateUtilisateurRequest;
import caisse.manager.caisse.dto.UpdateUtilisateurRequest;
import caisse.manager.caisse.dto.UtilisateurDTO;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UtilisateurDTO> getAllBySnackId(Long snackId) {
        return utilisateurRepository.findBySnackId(snackId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UtilisateurDTO create(CreateUtilisateurRequest request, Long snackId) {
        String normalizedUsername = normalizeUsername(request.getUsername());
        String normalizedRole = normalizeRole(request.getRole());

        if (utilisateurRepository.existsBySnackIdAndUsername(snackId, normalizedUsername)) {
            throw new RuntimeException("Ce nom d'utilisateur existe déjà dans ce restaurant");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Le mot de passe est requis");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(normalizedUsername);
        utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        utilisateur.setRole(normalizedRole);
        utilisateur.setSnackId(snackId);
        utilisateur.setActif(true);
        utilisateur.setDateCreation(LocalDateTime.now());

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return toDTO(saved);
    }

    @Transactional
    public UtilisateurDTO update(Long id, UpdateUtilisateurRequest request, Long snackId) {
        Utilisateur utilisateur = utilisateurRepository.findBySnackIdAndId(snackId, id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String normalizedUsername = normalizeUsername(request.getUsername());
            if (!normalizedUsername.equals(utilisateur.getUsername())) {
                if (utilisateurRepository.existsBySnackIdAndUsername(snackId, normalizedUsername)) {
                    throw new RuntimeException("Ce nom d'utilisateur existe déjà");
                }
                utilisateur.setUsername(normalizedUsername);
            }
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            utilisateur.setRole(normalizeRole(request.getRole()));
        }

        return toDTO(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void delete(Long id, Long snackId) {
        Utilisateur utilisateur = utilisateurRepository.findBySnackIdAndId(snackId, id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if ("ROLE_MANAGER".equals(utilisateur.getRole()) || "MANAGER".equals(utilisateur.getRole())) {
            throw new RuntimeException("Impossible de supprimer le manager principal");
        }

        utilisateurRepository.delete(utilisateur);
    }

    @Transactional
    public String resetPassword(Long id, Long snackId) {
        Utilisateur utilisateur = utilisateurRepository.findBySnackIdAndId(snackId, id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String tempPassword = generateTempPassword();
        utilisateur.setPassword(passwordEncoder.encode(tempPassword));
        utilisateurRepository.save(utilisateur);

        return tempPassword;
    }

    @Transactional
    public UtilisateurDTO toggleStatus(Long id, Long snackId) {
        Utilisateur utilisateur = utilisateurRepository.findBySnackIdAndId(snackId, id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        utilisateur.setActif(utilisateur.getActif() == null || !utilisateur.getActif());
        return toDTO(utilisateurRepository.save(utilisateur));
    }

    private UtilisateurDTO toDTO(Utilisateur u) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setRole(u.getRole());
        dto.setActif(u.getActif() != null ? u.getActif() : true);
        dto.setDateCreation(u.getDateCreation());
        return dto;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Le nom d'utilisateur est requis");
        }
        return username.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_CAISSIER";
        }
        String trimmed = role.trim().toUpperCase();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
