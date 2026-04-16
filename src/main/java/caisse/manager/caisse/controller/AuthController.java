package caisse.manager.caisse.controller;

import caisse.manager.caisse.dto.AuthRequest;
import caisse.manager.caisse.dto.AuthResponse;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.UtilisateurRepository;
import caisse.manager.caisse.security.JwtUtil;
import caisse.manager.caisse.security.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private UtilisateurRepository utilisateurRepository; // Pour récupérer le snackId

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            // 1. Tenter d'authentifier l'utilisateur (vérifie le mot de passe crypté)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Pseudo ou mot de passe incorrect", e);
        }

        // 2. Si l'authentification réussit, on charge les détails complets
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());

        // 3. On génère le Token JWT
        final String jwt = jwtUtil.generateToken(userDetails);

        Utilisateur utilisateur = utilisateurRepository.findByUsername(authRequest.getUsername()).get();

        return ResponseEntity.ok(new AuthResponse(
                jwt,
                utilisateur.getUsername(),
                utilisateur.getSnackId(),
                utilisateur.getRole()
        ));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        // Si on arrive ici, c'est que le token est valide (grâce au JwtFilter)
        return ResponseEntity.ok("Token valide");
    }
}
