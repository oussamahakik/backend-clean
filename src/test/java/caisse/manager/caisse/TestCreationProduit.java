package caisse.manager.caisse;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test spécifique pour vérifier la création de produits avec snackId
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Test création de produits avec snackId")
class TestCreationProduit {

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

    private String superAdminToken;
    private String managerToken;
    private Long snackId;

    @BeforeEach
    void setUp() {
        superAdminToken = null;
        managerToken = null;
        snackId = null;
    }

    @Test
    @DisplayName("🔧 TEST: Création de produit avec snackId - Correction du bug")
    void testCreationProduitAvecSnackId() throws Exception {
        // 1. Authentification Super Admin
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
        createSnackRequest.put("nomRestaurant", "Restaurant Test " + uniqueId);
        createSnackRequest.put("adresse", "123 Rue Test");
        createSnackRequest.put("usernameManager", "manager_" + uniqueId);
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
        managerLogin.put("username", "manager_" + uniqueId);
        managerLogin.put("password", "password123");

        MvcResult managerAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String managerAuthResponse = managerAuthResult.getResponse().getContentAsString();
        Map<String, Object> managerAuth = objectMapper.readValue(managerAuthResponse, Map.class);
        managerToken = (String) managerAuth.get("token");

        // 4. CRÉER PLUSIEURS PRODUITS (simulation création de menu)
        // Payload EXACT du Frontend (MenuAdmin.js ligne 41)
        
        // Produit 1 : Tacos XL
        Map<String, Object> produit1 = new HashMap<>();
        produit1.put("nom", "Tacos XL");
        produit1.put("prix", 10.0);
        produit1.put("categorie", "Tacos");
        produit1.put("disponible", true);

        MvcResult result1 = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produit1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nom").value("Tacos XL"))
                .andExpect(jsonPath("$.snackId").value(snackId.intValue()))
                .andReturn();

        String response1 = result1.getResponse().getContentAsString();
        Map<String, Object> produit1Response = objectMapper.readValue(response1, Map.class);
        Long produit1Id = ((Number) produit1Response.get("id")).longValue();

        // Vérification BDD
        Optional<Produit> produit1BDD = produitRepository.findById(produit1Id);
        assertTrue(produit1BDD.isPresent(), "Le produit 1 doit exister en BDD");
        assertEquals(snackId, produit1BDD.get().getSnackId(), "Le snackId doit être correctement sauvegardé");
        assertEquals("Tacos XL", produit1BDD.get().getNom());

        // Produit 2 : Burger Classic
        Map<String, Object> produit2 = new HashMap<>();
        produit2.put("nom", "Burger Classic");
        produit2.put("prix", 8.5);
        produit2.put("categorie", "Burgers");
        produit2.put("disponible", true);

        MvcResult result2 = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produit2)))
                .andExpect(status().isOk())
                .andReturn();

        String response2 = result2.getResponse().getContentAsString();
        Map<String, Object> produit2Response = objectMapper.readValue(response2, Map.class);
        Long produit2Id = ((Number) produit2Response.get("id")).longValue();

        // Vérification BDD
        Optional<Produit> produit2BDD = produitRepository.findById(produit2Id);
        assertTrue(produit2BDD.isPresent(), "Le produit 2 doit exister en BDD");
        assertEquals(snackId, produit2BDD.get().getSnackId(), "Le snackId doit être correctement sauvegardé");
        assertEquals("Burger Classic", produit2BDD.get().getNom());

        // Produit 3 : Coca-Cola
        Map<String, Object> produit3 = new HashMap<>();
        produit3.put("nom", "Coca-Cola");
        produit3.put("prix", 2.5);
        produit3.put("categorie", "Boissons");
        produit3.put("disponible", true);

        MvcResult result3 = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produit3)))
                .andExpect(status().isOk())
                .andReturn();

        String response3 = result3.getResponse().getContentAsString();
        Map<String, Object> produit3Response = objectMapper.readValue(response3, Map.class);
        Long produit3Id = ((Number) produit3Response.get("id")).longValue();

        // Vérification BDD
        Optional<Produit> produit3BDD = produitRepository.findById(produit3Id);
        assertTrue(produit3BDD.isPresent(), "Le produit 3 doit exister en BDD");
        assertEquals(snackId, produit3BDD.get().getSnackId(), "Le snackId doit être correctement sauvegardé");
        assertEquals("Coca-Cola", produit3BDD.get().getNom());

        // Vérifier que tous les produits ont le même snackId
        assertEquals(snackId, produit1BDD.get().getSnackId());
        assertEquals(snackId, produit2BDD.get().getSnackId());
        assertEquals(snackId, produit3BDD.get().getSnackId());

        // Vérifier qu'on peut récupérer tous les produits du snack
        var produitsDuSnack = produitRepository.findBySnackId(snackId);
        assertTrue(produitsDuSnack.size() >= 3, "Il doit y avoir au moins 3 produits pour ce snack");

        System.out.println("\n✅ TEST RÉUSSI : Création de 3 produits avec snackId correctement sauvegardé");
        System.out.println("✅ Produit 1: " + produit1BDD.get().getNom() + " (snackId: " + produit1BDD.get().getSnackId() + ")");
        System.out.println("✅ Produit 2: " + produit2BDD.get().getNom() + " (snackId: " + produit2BDD.get().getSnackId() + ")");
        System.out.println("✅ Produit 3: " + produit3BDD.get().getNom() + " (snackId: " + produit3BDD.get().getSnackId() + ")");
    }
}

