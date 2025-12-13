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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration complet de l'API CaisseManager
 * 
 * Scénario de test end-to-end :
 * 1. Authentification SUPER_ADMIN
 * 2. Création d'un restaurant (Snack) + Manager
 * 3. Authentification Manager
 * 4. Création d'ingrédients
 * 5. Création de produits
 * 6. Vérification des créations (GET)
 * 7. Création d'une commande complexe
 * 8. Vérification des commandes actives
 * 9. Mise à jour du statut de commande
 * 10. Test des settings
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'intégration complets de l'API")
class GlobalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String superAdminToken;
    private String managerToken;
    private Long snackId;
    private Long ingredientId;
    private Long produitId;
    private Long commandeId;
    private String managerUsername; // Pour stocker le nom d'utilisateur du manager créé

    @BeforeEach
    void setUp() throws Exception {
        // Le SUPER_ADMIN sera créé par DataInitializer au démarrage de l'application de test
        // Username: "hakik_owner", Password: "oussamahakikOwner"
        // Réinitialiser les variables d'instance
        superAdminToken = null;
        managerToken = null;
        snackId = null;
        ingredientId = null;
        produitId = null;
        commandeId = null;
    }

    @Test
    @DisplayName("ÉTAPE 1: Authentification SUPER_ADMIN")
    void testStep1_AuthenticateSuperAdmin() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "hakik_owner");
        loginRequest.put("password", "oussamahakikOwner");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andReturn();

        // Extract token
        String response = result.getResponse().getContentAsString();
        Map<String, Object> authResponse = objectMapper.readValue(response, Map.class);
        superAdminToken = (String) authResponse.get("token");

        // Assert
        assertNotNull(superAdminToken, "Le token SUPER_ADMIN doit être récupéré");
        assertFalse(superAdminToken.isEmpty(), "Le token ne doit pas être vide");
    }

    @Test
    @DisplayName("ÉTAPE 2: Création d'un Restaurant (Snack) + Manager")
    void testStep2_CreateSnackAndManager() throws Exception {
        // Arrange - D'abord s'authentifier
        testStep1_AuthenticateSuperAdmin();

        // Utiliser un timestamp pour garantir l'unicité
        String uniqueId = String.valueOf(System.currentTimeMillis());
        managerUsername = "manager_test_" + uniqueId;

        Map<String, String> createSnackRequest = new HashMap<>();
        createSnackRequest.put("nomRestaurant", "Test Snack API " + uniqueId);
        createSnackRequest.put("adresse", "123 Rue Test, 7000 Mons");
        createSnackRequest.put("usernameManager", managerUsername);
        createSnackRequest.put("passwordManager", "password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/super-admin/snacks")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSnackRequest)))
                .andReturn();
        
        // Debug: Afficher la réponse en cas d'erreur
        int status = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();
        
        if (status != 200) {
            System.err.println("❌ ERREUR lors de la création du snack:");
            System.err.println("Status: " + status);
            System.err.println("Response: " + responseBody);
            System.err.println("Request: " + objectMapper.writeValueAsString(createSnackRequest));
        }
        
        // Assert
        assertEquals(200, status, "La création du snack doit retourner 200 OK. Réponse: " + responseBody);

        // Assert - Vérifier que la réponse contient le message de succès
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("créé avec succès"), "Le snack doit être créé avec succès. Réponse: " + response);

        // Extraire le snackId depuis la réponse (format: "Restaurant 'Nom' créé avec succès (ID: 123)")
        // Ou récupérer en listant les snacks
        try {
            // Essayer d'extraire l'ID depuis la réponse
            if (response.contains("ID: ")) {
                String idPart = response.substring(response.indexOf("ID: ") + 4);
                idPart = idPart.substring(0, idPart.indexOf(")"));
                snackId = Long.parseLong(idPart.trim());
            }
        } catch (Exception e) {
            // Si l'extraction échoue, récupérer en listant
        }

        // Si snackId n'est pas encore défini, le récupérer en listant
        if (snackId == null) {
            MvcResult snacksResult = mockMvc.perform(get("/api/super-admin/snacks")
                            .header("Authorization", "Bearer " + superAdminToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String snacksResponse = snacksResult.getResponse().getContentAsString();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> snacks = objectMapper.readValue(snacksResponse, List.class);
            
            // Trouver le snack créé (le dernier créé ou celui avec le bon nom)
            String expectedNom = "Test Snack API " + uniqueId;
            for (Map<String, Object> snack : snacks) {
                if (expectedNom.equals(snack.get("nom"))) {
                    Object idObj = snack.get("id");
                    if (idObj instanceof Number) {
                        snackId = ((Number) idObj).longValue();
                        break;
                    }
                }
            }
        }

        assertNotNull(snackId, "Le snackId doit être récupéré après création. Réponse création: " + response);

        // Se connecter avec le manager créé
        Map<String, String> managerLogin = new HashMap<>();
        managerLogin.put("username", managerUsername);
        managerLogin.put("password", "password123");

        MvcResult managerAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snackId").value(snackId))
                .andReturn();

        String managerResponse = managerAuthResult.getResponse().getContentAsString();
        Map<String, Object> managerAuth = objectMapper.readValue(managerResponse, Map.class);
        managerToken = (String) managerAuth.get("token");

        assertNotNull(managerToken, "Le token Manager doit être récupéré");
    }

    @Test
    @DisplayName("ÉTAPE 3: Création d'Ingrédients")
    void testStep3_CreateIngredient() throws Exception {
        // Arrange - Préparer les étapes précédentes
        testStep2_CreateSnackAndManager();

        Map<String, Object> ingredientRequest = new HashMap<>();
        ingredientRequest.put("nom", "Sauce Algérienne");
        ingredientRequest.put("type", "SAUCE");
        ingredientRequest.put("prixSupplement", 0.5);
        ingredientRequest.put("disponible", true);

        // Act
        MvcResult result = mockMvc.perform(post("/api/ingredients")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingredientRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nom").value("Sauce Algérienne"))
                .andReturn();

        // Extract ingredient ID
        String response = result.getResponse().getContentAsString();
        Map<String, Object> ingredient = objectMapper.readValue(response, Map.class);
        ingredientId = ((Number) ingredient.get("id")).longValue();

        // Assert
        assertNotNull(ingredientId, "L'ingrédient doit avoir un ID après création");

        // Vérification : GET pour s'assurer que l'ingrédient est bien en base
        mockMvc.perform(get("/api/ingredients")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + ingredientId + ")]").exists())
                .andExpect(jsonPath("$[?(@.nom == 'Sauce Algérienne')]").exists());
    }

    @Test
    @DisplayName("ÉTAPE 4: Création de Produits")
    void testStep4_CreateProduct() throws Exception {
        // Arrange
        testStep3_CreateIngredient();

        Map<String, Object> produitRequest = new HashMap<>();
        produitRequest.put("nom", "Tacos XL");
        produitRequest.put("prix", 10.0);
        produitRequest.put("categorie", "Tacos");
        produitRequest.put("disponible", true);

        // Act
        MvcResult result = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nom").value("Tacos XL"))
                .andExpect(jsonPath("$.prix").value(10.0))
                .andReturn();

        // Extract product ID
        String response = result.getResponse().getContentAsString();
        Map<String, Object> produit = objectMapper.readValue(response, Map.class);
        produitId = ((Number) produit.get("id")).longValue();

        // Assert
        assertNotNull(produitId, "Le produit doit avoir un ID après création");

        // Vérification CRITIQUE : GET pour s'assurer que le produit est bien en base
        MvcResult getResult = mockMvc.perform(get("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String getResponse = getResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> produits = objectMapper.readValue(getResponse, List.class);
        
        boolean produitTrouve = produits.stream()
                .anyMatch(p -> {
                    Object idObj = p.get("id");
                    Object nomObj = p.get("nom");
                    return idObj instanceof Number 
                            && produitId.equals(((Number) idObj).longValue()) 
                            && "Tacos XL".equals(nomObj);
                });

        assertTrue(produitTrouve, "Le produit créé doit être présent dans la liste GET /api/produits");
    }

    @Test
    @DisplayName("ÉTAPE 5: Création d'une Commande Complexe")
    void testStep5_CreateComplexOrder() throws Exception {
        // Arrange
        testStep4_CreateProduct();

        // Créer un produit simple (Coca) d'abord
        Map<String, Object> cocaRequest = new HashMap<>();
        cocaRequest.put("nom", "Coca-Cola");
        cocaRequest.put("prix", 2.5);
        cocaRequest.put("categorie", "Boissons");
        cocaRequest.put("disponible", true);

        MvcResult cocaResult = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cocaRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String cocaResponse = cocaResult.getResponse().getContentAsString();
        Map<String, Object> coca = objectMapper.readValue(cocaResponse, Map.class);
        Long cocaId = ((Number) coca.get("id")).longValue();

        // Construire la commande avec :
        // - Un produit simple (Coca)
        // - Un produit complexe (Tacos XL) avec détails et prixFinal
        Map<String, Object> ligne1 = new HashMap<>();
        ligne1.put("produitId", cocaId);
        ligne1.put("quantite", 2);
        ligne1.put("details", "");
        ligne1.put("prixFinal", 2.5);

        Map<String, Object> ligne2 = new HashMap<>();
        ligne2.put("produitId", produitId);
        ligne2.put("quantite", 1);
        ligne2.put("details", "Sans frites, Sauce Algérienne");
        ligne2.put("prixFinal", 10.5); // Prix de base + supplément

        Map<String, Object> commandeRequest = new HashMap<>();
        commandeRequest.put("articles", List.of(ligne1, ligne2));
        commandeRequest.put("typePaiement", "CARTE");
        commandeRequest.put("remise", 0.0);

        // Act
        MvcResult result = mockMvc.perform(post("/api/commandes")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("enregistrée"), "La commande doit être enregistrée avec succès");

        // Extraire l'ID de la commande depuis la réponse
        // Format attendu: "Commande #123 enregistrée !"
        String[] parts = response.split("#");
        if (parts.length > 1) {
            String idPart = parts[1].split(" ")[0];
            commandeId = Long.parseLong(idPart);
        }

        assertNotNull(commandeId, "La commande doit avoir un ID");
    }

    @Test
    @DisplayName("ÉTAPE 6: Vérification Commandes Actives")
    void testStep6_GetActiveOrders() throws Exception {
        // Arrange
        testStep5_CreateComplexOrder();

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/commandes/actives")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commandes = objectMapper.readValue(response, List.class);

        boolean commandeTrouvee = commandes.stream()
                .anyMatch(c -> {
                    Object idObj = c.get("id");
                    return idObj instanceof Number 
                            && commandeId.equals(((Number) idObj).longValue());
                });

        assertTrue(commandeTrouvee, "La commande créée doit apparaître dans les commandes actives");
    }

    @Test
    @DisplayName("ÉTAPE 7: Mise à jour du Statut de Commande")
    void testStep7_UpdateOrderStatus() throws Exception {
        // Arrange
        testStep6_GetActiveOrders();

        // Act
        mockMvc.perform(put("/api/commandes/" + commandeId + "/statut")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .param("nouveauStatut", "PRETE"))
                .andExpect(status().isOk());

        // Vérifier que le statut a changé
        MvcResult result = mockMvc.perform(get("/api/commandes/actives")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commandes = objectMapper.readValue(response, List.class);

        // La commande ne doit plus être EN_ATTENTE
        boolean encoreEnAttente = commandes.stream()
                .anyMatch(c -> {
                    Object idObj = c.get("id");
                    Object statutObj = c.get("statut");
                    return idObj instanceof Number 
                            && commandeId.equals(((Number) idObj).longValue())
                            && "EN_ATTENTE".equals(statutObj);
                });

        assertFalse(encoreEnAttente, "La commande ne doit plus être EN_ATTENTE après mise à jour");
    }

    @Test
    @DisplayName("ÉTAPE 8: Test GET Settings (Vérification 500)")
    void testStep8_GetSnackSettings() throws Exception {
        // Arrange
        testStep2_CreateSnackAndManager();
        
        // Vérifier que snackId et managerToken sont bien définis
        assertNotNull(snackId, "snackId doit être défini après testStep2");
        assertNotNull(managerToken, "managerToken doit être défini après testStep2");
        assertTrue(snackId > 0, "snackId doit être un nombre positif");

        // Act & Assert - Vérifier qu'il n'y a pas d'erreur 500
        // Utiliser UriComponentsBuilder pour construire l'URL proprement
        java.net.URI uri = java.net.URI.create("/api/snacks/" + snackId + "/settings");
        
        mockMvc.perform(get(uri)
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", String.valueOf(snackId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").exists())
                .andExpect(jsonPath("$.adresse").exists());
    }

    @Test
    @DisplayName("SCÉNARIO COMPLET: Tous les tests en séquence")
    void testCompleteScenario() throws Exception {
        // Exécuter tous les tests dans l'ordre
        testStep1_AuthenticateSuperAdmin();
        testStep2_CreateSnackAndManager();
        testStep3_CreateIngredient();
        testStep4_CreateProduct();
        testStep5_CreateComplexOrder();
        testStep6_GetActiveOrders();
        testStep7_UpdateOrderStatus();
        testStep8_GetSnackSettings();

        // Si on arrive ici, tous les tests sont passés
        System.out.println("\n✅ TOUS LES TESTS SONT PASSÉS !");
        System.out.println("✅ La création de Snack fonctionne.");
        System.out.println("✅ La création de Produits fonctionne.");
        System.out.println("✅ La création d'Ingrédients fonctionne.");
        System.out.println("✅ La prise de Commande fonctionne.");
        System.out.println("✅ Aucune erreur 500 ou 403 détectée.");
    }
}

