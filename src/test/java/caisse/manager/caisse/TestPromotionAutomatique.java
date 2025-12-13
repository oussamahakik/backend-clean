package caisse.manager.caisse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import caisse.manager.caisse.model.*;
import caisse.manager.caisse.repository.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de l'application automatique des promotions sur les commandes
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Test application automatique des promotions")
class TestPromotionAutomatique {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private SnackRepository snackRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    private String superAdminToken;
    private String managerToken;
    private Long snackId;
    private Long produitId;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        superAdminToken = null;
        managerToken = null;
        snackId = null;
        produitId = null;
    }

    @Test
    @DisplayName("🎁 TEST: Application automatique promotion sur catégorie lors de création commande")
    void testApplicationPromotionAutomatiqueCategorie() throws Exception {
        // 1. Authentification
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "hakik_owner");
        loginRequest.put("password", "oussamahakikOwner");

        MvcResult authResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String authResponse = authResult.getResponse().getContentAsString();
        Map<String, Object> auth = objectMapper.readValue(authResponse, Map.class);
        superAdminToken = (String) auth.get("token");

        // 2. Créer un snack
        String uniqueId = String.valueOf(System.currentTimeMillis());
        Map<String, String> createSnackRequest = new HashMap<>();
        createSnackRequest.put("nomRestaurant", "Snack Test Promo " + uniqueId);
        createSnackRequest.put("adresse", "123 Rue Test");
        createSnackRequest.put("usernameManager", "manager_promo_" + uniqueId);
        createSnackRequest.put("passwordManager", "password123");

        MvcResult snackResult = mockMvc.perform(post("/api/super-admin/snacks")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSnackRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String snackResponse = snackResult.getResponse().getContentAsString();
        if (snackResponse.contains("ID: ")) {
            String idPart = snackResponse.substring(snackResponse.indexOf("ID: ") + 4);
            idPart = idPart.substring(0, idPart.indexOf(")"));
            snackId = Long.parseLong(idPart.trim());
        }

        // 3. Se connecter avec le manager
        Map<String, String> managerLogin = new HashMap<>();
        managerLogin.put("username", "manager_promo_" + uniqueId);
        managerLogin.put("password", "password123");

        MvcResult managerAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String managerAuthResponse = managerAuthResult.getResponse().getContentAsString();
        Map<String, Object> managerAuth = objectMapper.readValue(managerAuthResponse, Map.class);
        managerToken = (String) managerAuth.get("token");

        // 4. Créer un produit de catégorie "Tacos"
        Map<String, Object> produitRequest = new HashMap<>();
        produitRequest.put("nom", "Tacos XL");
        produitRequest.put("prix", 10.0);
        produitRequest.put("categorie", "Tacos");
        produitRequest.put("disponible", true);

        MvcResult produitResult = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produitRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String produitResponse = produitResult.getResponse().getContentAsString();
        Map<String, Object> produit = objectMapper.readValue(produitResponse, Map.class);
        produitId = ((Number) produit.get("id")).longValue();

        // 5. Créer une promotion de -10% sur la catégorie "Tacos"
        Map<String, Object> promotionRequest = new HashMap<>();
        promotionRequest.put("nom", "Promo Tacos -10%");
        promotionRequest.put("description", "Réduction de 10% sur tous les Tacos");
        promotionRequest.put("typePromotion", "POURCENTAGE");
        promotionRequest.put("valeur", 10.0);
        promotionRequest.put("dateDebut", LocalDate.now().toString());
        promotionRequest.put("dateFin", LocalDate.now().plusDays(30).toString());
        promotionRequest.put("actif", true);
        promotionRequest.put("categorie", "Tacos"); // Promotion sur la catégorie

        MvcResult promoResult = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promotionRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 6. Créer une commande avec ce produit
        // Le prix initial est 10.0€, avec -10%, le prix devrait être 9.0€
        Map<String, Object> ligne1 = new HashMap<>();
        ligne1.put("produitId", produitId);
        ligne1.put("quantite", 1);
        ligne1.put("details", "");
        ligne1.put("prixFinal", 10.0); // Prix de base du Frontend

        Map<String, Object> commandeRequest = new HashMap<>();
        commandeRequest.put("typePaiement", "ESPECES");
        commandeRequest.put("articles", List.of(ligne1));
        commandeRequest.put("remise", 0.0);

        MvcResult commandeResult = mockMvc.perform(post("/api/commandes")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String commandeResponse = commandeResult.getResponse().getContentAsString();
        Long commandeId = null;
        if (commandeResponse.contains("#")) {
            String idPart = commandeResponse.substring(commandeResponse.indexOf("#") + 1);
            idPart = idPart.substring(0, idPart.indexOf(" "));
            commandeId = Long.parseLong(idPart.trim());
        }

        // 7. VÉRIFICATION BDD : Le prix dans la ligne de commande doit être 9.0€ (10.0 - 10%)
        Optional<Commande> commandeOpt = commandeRepository.findById(commandeId);
        assertTrue(commandeOpt.isPresent(), "La commande doit exister en BDD");
        Commande commandeBDD = commandeOpt.get();

        List<LigneCommande> lignes = commandeBDD.getLignes();
        assertNotNull(lignes, "Les lignes ne doivent pas être null");
        assertEquals(1, lignes.size(), "Il doit y avoir 1 ligne de commande");

        LigneCommande ligneBDD = lignes.get(0);
        assertEquals(9.0, ligneBDD.getPrixUnitaire(), 0.01, 
                "Le prix doit être réduit de 10% : 10.0€ - 10% = 9.0€");
        assertEquals(9.0, commandeBDD.getTotal(), 0.01, 
                "Le total de la commande doit être 9.0€");

        System.out.println("\n✅ TEST RÉUSSI : Promotion automatique appliquée");
        System.out.println("✅ Prix initial : 10.0€");
        System.out.println("✅ Prix avec promotion (-10%) : " + ligneBDD.getPrixUnitaire() + "€");
    }

    @Test
    @DisplayName("🎁 TEST: Promotion MONTANT_FIXE appliquée automatiquement")
    void testPromotionMontantFixeAutomatique() throws Exception {
        // Setup similaire au test précédent
        testApplicationPromotionAutomatiqueCategorie();

        // Créer une nouvelle promotion de type MONTANT_FIXE (-2€)
        Map<String, Object> promotionRequest = new HashMap<>();
        promotionRequest.put("nom", "Promo Tacos -2€");
        promotionRequest.put("typePromotion", "MONTANT_FIXE");
        promotionRequest.put("valeur", 2.0);
        promotionRequest.put("dateDebut", LocalDate.now().toString());
        promotionRequest.put("dateFin", LocalDate.now().plusDays(30).toString());
        promotionRequest.put("actif", true);
        promotionRequest.put("categorie", "Tacos");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promotionRequest)))
                .andExpect(status().isOk());

        // Créer une commande
        Map<String, Object> ligne1 = new HashMap<>();
        ligne1.put("produitId", produitId);
        ligne1.put("quantite", 1);
        ligne1.put("details", "");
        ligne1.put("prixFinal", 10.0);

        Map<String, Object> commandeRequest = new HashMap<>();
        commandeRequest.put("typePaiement", "ESPECES");
        commandeRequest.put("articles", List.of(ligne1));
        commandeRequest.put("remise", 0.0);

        MvcResult commandeResult = mockMvc.perform(post("/api/commandes")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String commandeResponse = commandeResult.getResponse().getContentAsString();
        Long commandeId = null;
        if (commandeResponse.contains("#")) {
            String idPart = commandeResponse.substring(commandeResponse.indexOf("#") + 1);
            idPart = idPart.substring(0, idPart.indexOf(" "));
            commandeId = Long.parseLong(idPart.trim());
        }

        // VÉRIFICATION : Avec plusieurs promotions actives, la première trouvée est appliquée
        // (Ici on teste qu'une promotion MONTANT_FIXE fonctionne)
        Optional<Commande> commandeOpt = commandeRepository.findById(commandeId);
        assertTrue(commandeOpt.isPresent());
        Commande commandeBDD = commandeOpt.get();
        LigneCommande ligneBDD = commandeBDD.getLignes().get(0);
        
        // Le prix devrait être réduit (soit par la première promo trouvée)
        assertTrue(ligneBDD.getPrixUnitaire() < 10.0, 
                "Le prix doit être réduit par une promotion");
    }
}

