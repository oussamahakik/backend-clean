 package caisse.manager.caisse.service;

import caisse.manager.caisse.dto.*;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Vérifier unicité username
        if (utilisateurRepository.existsBySnackIdAndUsername(snackId, request.getUsername())) {
            throw new RuntimeException("Ce nom d'utilisateur existe déjà dans ce restaurant");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(request.getUsername());
        utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        utilisateur.setRole(request.getRole() != null ? request.getRole() : "ROLE_CAISSIER");
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

        if (request.getUsername() != null && !request.getUsername().equals(utilisateur.getUsername())) {
            if (utilisateurRepository.existsBySnackIdAndUsername(snackId, request.getUsername())) {
                throw new RuntimeException("Ce nom d'utilisateur existe déjà");
            }
            utilisateur.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            utilisateur.setRole(request.getRole());
        }

        return toDTO(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void delete(Long id, Long snackId) {
        Utilisateur utilisateur = utilisateurRepository.findBySnackIdAndId(snackId, id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // Vérifier que ce n'est pas le manager principal
        if ("ROLE_MANAGER".equals(utilisateur.getRole()) || "MANAGER".equals(utilisateur.getRole())) {
            throw new RuntimeException("Impossible de supprimer le manager principal");
        }

        utilisateurRepository.delete(utilisateur);
    }

    @Transactional
    public String resetPassword(Long id, Long snackId) {
        Utilisateur utilisateur = utilisateurRepository.findBySnackIdAndId(snackId, id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Générer mot de passe temporaire (8 caractères aléatoires)
        String tempPassword = generateTempPassword();
        utilisateur.setPassword(passwordEncoder.encode(tempPassword));
        utilisateurRepository.save(utilisateur);

        return tempPassword; // Retourner en clair pour affichage
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












