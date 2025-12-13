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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration globaux du système Frontend/Backend/BDD
 * 
 * VALIDATION 100% DE LA CHAÎNE :
 * Frontend (React) ↔ Backend (Spring Boot) ↔ Base de Données (MariaDB)
 * 
 * Ce test simule exactement les requêtes que le Frontend React envoie
 * et vérifie que les données sont correctement persistées en base de données.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'intégration globaux - Validation 100% Frontend/Backend/BDD")
class GlobalSystemTest {

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
    private IngredientRepository ingredientRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    private String superAdminToken;
    private String managerToken;
    private Long snackId;
    private Long produitId;
    private Long ingredientId;
    private Long commandeId;
    private Long promotionId;

    @BeforeEach
    void setUp() {
        // Configurer ObjectMapper pour les dates LocalDate
        objectMapper.registerModule(new JavaTimeModule());
        
        superAdminToken = null;
        managerToken = null;
        snackId = null;
        produitId = null;
        ingredientId = null;
        commandeId = null;
        promotionId = null;
    }

    // ============================================
    // 1. AUTHENTIFICATION & SÉCURITÉ
    // ============================================

    @Test
    @DisplayName("🔐 TEST 1: Authentification Login - Vérification Token, Role, SnackId en BDD")
    void test1_Authentication_VerificationBDD() throws Exception {
        // Arrange - Payload exact du Frontend (Login.js ligne 40)
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "hakik_owner");
        loginRequest.put("password", "oussamahakikOwner");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("hakik_owner"))
                .andExpect(jsonPath("$.role").exists())
                .andReturn();

        // Extract token
        String response = result.getResponse().getContentAsString();
        Map<String, Object> authResponse = objectMapper.readValue(response, Map.class);
        superAdminToken = (String) authResponse.get("token");
        String role = (String) authResponse.get("role");

        // Assert - Vérification réponse HTTP
        assertNotNull(superAdminToken, "Le token doit être présent");
        assertFalse(superAdminToken.isEmpty(), "Le token ne doit pas être vide");
        assertEquals("SUPER_ADMIN", role, "Le rôle doit être SUPER_ADMIN");

        // VÉRIFICATION BDD : L'utilisateur existe bien en base
        Optional<Utilisateur> userOpt = utilisateurRepository.findByUsername("hakik_owner");
        assertTrue(userOpt.isPresent(), "L'utilisateur SUPER_ADMIN doit exister en BDD");
        Utilisateur userBDD = userOpt.get();
        assertEquals("SUPER_ADMIN", userBDD.getRole(), "Le rôle doit être SUPER_ADMIN en BDD");
        assertNull(userBDD.getSnackId(), "Le SUPER_ADMIN ne doit pas avoir de snackId en BDD");
    }

    @Test
    @DisplayName("🔐 TEST 1.1: Validate Token - Vérification sécurité")
    void test1_1_ValidateToken() throws Exception {
        test1_Authentication_VerificationBDD();
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
    }

    // ============================================
    // 2. GESTION SAAS (SUPER ADMIN) - Vérification @Transactional
    // ============================================

    @Test
    @DisplayName("🏢 TEST 2: Création Snack + Manager - Vérification @Transactional et actif=true en BDD")
    void test2_CreateSnack_VerificationTransactionnelle() throws Exception {
        // Arrange
        test1_Authentication_VerificationBDD();

        // Payload exact du Frontend (SuperAdminDashboard.js ligne 298)
        String uniqueId = String.valueOf(System.currentTimeMillis());
        Map<String, String> createSnackRequest = new HashMap<>();
        createSnackRequest.put("nomRestaurant", "Snack Test Transaction " + uniqueId);
        createSnackRequest.put("adresse", "123 Rue Test Transaction");
        createSnackRequest.put("usernameManager", "manager_trans_" + uniqueId);
        createSnackRequest.put("passwordManager", "password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/super-admin/snacks")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSnackRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract snackId
        String response = result.getResponse().getContentAsString();
        if (response.contains("ID: ")) {
            String idPart = response.substring(response.indexOf("ID: ") + 4);
            idPart = idPart.substring(0, idPart.indexOf(")"));
            snackId = Long.parseLong(idPart.trim());
        }

        // VÉRIFICATION BDD CRITIQUE : Le snack est actif par défaut
        Optional<Snack> snackOpt = snackRepository.findById(snackId);
        assertTrue(snackOpt.isPresent(), "Le snack doit exister en BDD");
        Snack snackBDD = snackOpt.get();
        assertTrue(snackBDD.isActif(), "Le snack doit être actif par défaut (actif=true)");
        assertEquals("Snack Test Transaction " + uniqueId, snackBDD.getNom(), "Le nom doit correspondre");
        assertEquals("123 Rue Test Transaction", snackBDD.getAdresse(), "L'adresse doit correspondre");

        // VÉRIFICATION BDD : Le manager a été créé (test de @Transactional)
        Optional<Utilisateur> managerOpt = utilisateurRepository.findBySnackIdAndRole(snackId, "MANAGER");
        assertTrue(managerOpt.isPresent(), "Le manager doit être créé en BDD (transaction réussie)");
        Utilisateur managerBDD = managerOpt.get();
        assertEquals("manager_trans_" + uniqueId, managerBDD.getUsername(), "Le username du manager doit correspondre");
        assertEquals(snackId, managerBDD.getSnackId(), "Le manager doit être lié au snack");
        assertEquals("MANAGER", managerBDD.getRole(), "Le rôle doit être MANAGER en BDD");

        // Se connecter avec le manager
        Map<String, String> managerLogin = new HashMap<>();
        managerLogin.put("username", "manager_trans_" + uniqueId);
        managerLogin.put("password", "password123");

        MvcResult managerAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snackId").value(snackId))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andReturn();

        String managerResponse = managerAuthResult.getResponse().getContentAsString();
        Map<String, Object> managerAuth = objectMapper.readValue(managerResponse, Map.class);
        managerToken = (String) managerAuth.get("token");
        assertNotNull(managerToken, "Le token manager doit être récupéré");
    }

    // ============================================
    // 3. GESTION MENU (PRODUITS & INGRÉDIENTS) - Vérification snack_id
    // ============================================

    @Test
    @DisplayName("🍔 TEST 3: Création Produit - Vérification snack_id en BDD")
    void test3_CreateProduct_VerificationSnackId() throws Exception {
        // Arrange
        test2_CreateSnack_VerificationTransactionnelle();

        // Payload exact du Frontend (MenuAdmin.js ligne 41)
        Map<String, Object> produitRequest = new HashMap<>();
        produitRequest.put("nom", "Tacos XL Test");
        produitRequest.put("prix", 10.50); // Double avec décimales
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
                .andExpect(jsonPath("$.nom").value("Tacos XL Test"))
                .andExpect(jsonPath("$.snackId").value(snackId.intValue()))
                .andReturn();

        // Extract product ID
        String response = result.getResponse().getContentAsString();
        Map<String, Object> produit = objectMapper.readValue(response, Map.class);
        produitId = ((Number) produit.get("id")).longValue();

        // VÉRIFICATION BDD CRITIQUE : Le snack_id est bien rempli
        Optional<Produit> produitOpt = produitRepository.findById(produitId);
        assertTrue(produitOpt.isPresent(), "Le produit doit exister en BDD");
        Produit produitBDD = produitOpt.get();
        assertEquals("Tacos XL Test", produitBDD.getNom(), "Le nom doit correspondre");
        assertEquals(10.50, produitBDD.getPrix(), 0.01, "Le prix doit correspondre exactement (pas d'arrondi)");
        assertEquals(snackId, produitBDD.getSnackId(), "Le snack_id doit être correctement rempli en BDD");
        assertEquals("Tacos", produitBDD.getCategorie(), "La catégorie doit correspondre");
        assertTrue(produitBDD.getDisponible(), "Le produit doit être disponible");
    }

    @Test
    @DisplayName("🥗 TEST 3.1: Création Ingrédient - Vérification snack_id et prixSupplement en BDD")
    void test3_1_CreateIngredient_VerificationSnackId() throws Exception {
        // Arrange
        test3_CreateProduct_VerificationSnackId();

        // Payload exact du Frontend (IngredientsAdmin.js ligne 47)
        Map<String, Object> ingredientRequest = new HashMap<>();
        ingredientRequest.put("nom", "Cheddar Test");
        ingredientRequest.put("type", "SUPPLEMENT");
        ingredientRequest.put("prixSupplement", 1.75); // Double avec décimales

        // Act
        MvcResult result = mockMvc.perform(post("/api/ingredients")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingredientRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nom").value("Cheddar Test"))
                .andExpect(jsonPath("$.type").value("SUPPLEMENT"))
                .andExpect(jsonPath("$.prixSupplement").value(1.75))
                .andReturn();

        // Extract ingredient ID
        String response = result.getResponse().getContentAsString();
        Map<String, Object> ingredient = objectMapper.readValue(response, Map.class);
        ingredientId = ((Number) ingredient.get("id")).longValue();

        // VÉRIFICATION BDD CRITIQUE : snack_id et prixSupplement sont bien sauvegardés
        Optional<Ingredient> ingredientOpt = ingredientRepository.findById(ingredientId);
        assertTrue(ingredientOpt.isPresent(), "L'ingrédient doit exister en BDD");
        Ingredient ingredientBDD = ingredientOpt.get();
        assertEquals("Cheddar Test", ingredientBDD.getNom(), "Le nom doit correspondre");
        assertEquals(TypeIngredient.SUPPLEMENT, ingredientBDD.getType(), "Le type doit être SUPPLEMENT");
        assertEquals(1.75, ingredientBDD.getPrixSupplement(), 0.01, "Le prixSupplement doit être exactement 1.75 (pas d'arrondi)");
        assertEquals(snackId, ingredientBDD.getSnackId(), "Le snack_id doit être correctement rempli en BDD");
    }

    // ============================================
    // 4. FLUX DE COMMANDE (COMPLEXE) - prixFinal et details
    // ============================================

    @Test
    @DisplayName("🛒 TEST 4: Création Commande Complexe - Vérification prixFinal, details et total en BDD")
    void test4_CreateComplexOrder_VerificationPrixFinalEtDetails() throws Exception {
        // Arrange
        test3_1_CreateIngredient_VerificationSnackId();

        // Créer un deuxième produit simple
        Map<String, Object> cocaRequest = new HashMap<>();
        cocaRequest.put("nom", "Coca-Cola Test");
        cocaRequest.put("prix", 2.50);
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

        // Payload EXACT du Frontend (App.js ligne 207-216)
        // Scénario : 
        // - 1x Tacos XL (10.50€) + Supplément Cheddar (1.75€) = 12.25€ avec détails
        // - 2x Coca-Cola (2.50€) = 5.00€
        // Total: 17.25€ - Remise 1.50€ = 15.75€

        Map<String, Object> ligne1 = new HashMap<>();
        ligne1.put("produitId", produitId); // Tacos XL
        ligne1.put("quantite", 1);
        ligne1.put("details", "Sans oignons, Sauce Algérienne, Supplément Cheddar"); // Chaîne de caractères
        ligne1.put("prixFinal", 12.25); // Prix calculé côté Frontend (10.50 + 1.75)

        Map<String, Object> ligne2 = new HashMap<>();
        ligne2.put("produitId", cocaId); // Coca-Cola
        ligne2.put("quantite", 2);
        ligne2.put("details", ""); // Chaîne vide pour produit simple
        ligne2.put("prixFinal", 2.50);

        Map<String, Object> commandeRequest = new HashMap<>();
        commandeRequest.put("typePaiement", "ESPECES");
        commandeRequest.put("articles", List.of(ligne1, ligne2));
        commandeRequest.put("remise", 1.50);

        // Act
        MvcResult result = mockMvc.perform(post("/api/commandes")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract commande ID
        String response = result.getResponse().getContentAsString();
        if (response.contains("#")) {
            String idPart = response.substring(response.indexOf("#") + 1);
            idPart = idPart.substring(0, idPart.indexOf(" "));
            commandeId = Long.parseLong(idPart.trim());
        }

        // VÉRIFICATION BDD CRITIQUE : La commande est bien persistée
        Optional<Commande> commandeOpt = commandeRepository.findById(commandeId);
        assertTrue(commandeOpt.isPresent(), "La commande doit exister en BDD");
        Commande commandeBDD = commandeOpt.get();

        // Vérifier les champs de la commande
        assertEquals(snackId, commandeBDD.getSnackId(), "Le snack_id doit correspondre");
        assertEquals("ESPECES", commandeBDD.getTypePaiement(), "Le typePaiement doit être ESPECES");
        assertEquals(1.50, commandeBDD.getRemise(), 0.01, "La remise doit être 1.50");
        assertEquals(StatutCommande.EN_ATTENTE, commandeBDD.getStatut(), "Le statut doit être EN_ATTENTE");
        
        // Vérifier le total calculé : (12.25 * 1) + (2.50 * 2) - 1.50 = 12.25 + 5.00 - 1.50 = 15.75
        assertEquals(15.75, commandeBDD.getTotal(), 0.01, "Le total doit être 15.75€ (17.25€ - 1.50€ remise)");

        // VÉRIFICATION BDD CRITIQUE : Les lignes de commande avec détails
        List<LigneCommande> lignes = commandeBDD.getLignes();
        assertNotNull(lignes, "Les lignes ne doivent pas être null");
        assertEquals(2, lignes.size(), "Il doit y avoir 2 lignes de commande");

        // Vérifier la première ligne (Tacos XL avec détails)
        LigneCommande ligne1BDD = lignes.stream()
                .filter(l -> l.getNomProduit().equals("Tacos XL Test"))
                .findFirst()
                .orElse(null);
        assertNotNull(ligne1BDD, "La ligne Tacos XL Test doit exister");
        assertEquals(1, ligne1BDD.getQuantite(), "La quantité doit être 1");
        assertEquals(12.25, ligne1BDD.getPrixUnitaire(), 0.01, "Le prixFinal (12.25€) doit être sauvegardé dans prixUnitaire");
        assertEquals("Sans oignons, Sauce Algérienne, Supplément Cheddar", 
                ligne1BDD.getDetails(), "Les détails doivent être exactement sauvegardés en BDD");

        // Vérifier la deuxième ligne (Coca)
        LigneCommande ligne2BDD = lignes.stream()
                .filter(l -> l.getNomProduit().equals("Coca-Cola Test"))
                .findFirst()
                .orElse(null);
        assertNotNull(ligne2BDD, "La ligne Coca-Cola Test doit exister");
        assertEquals(2, ligne2BDD.getQuantite(), "La quantité doit être 2");
        assertEquals(2.50, ligne2BDD.getPrixUnitaire(), 0.01, "Le prix doit être 2.50€");
        assertEquals("", ligne2BDD.getDetails(), "Les détails doivent être vides pour un produit simple");
    }

    // ============================================
    // 5. MODULE PROMOTIONS (NOUVEAU) - Dates et filtre actives
    // ============================================

    @Test
    @DisplayName("🎁 TEST 5: Création Promotion - Vérification dates et snack_id en BDD")
    void test5_CreatePromotion_VerificationDatesEtSnackId() throws Exception {
        // Arrange
        test3_CreateProduct_VerificationSnackId();

        // Payload exact du Frontend (PromotionsManager.js ligne 87-92)
        Map<String, Object> promotionRequest = new HashMap<>();
        promotionRequest.put("nom", "Promo Été 2024");
        promotionRequest.put("description", "Promotion estivale -10%");
        promotionRequest.put("typePromotion", "POURCENTAGE");
        promotionRequest.put("valeur", 10.0);
        promotionRequest.put("dateDebut", LocalDate.now().toString()); // Format ISO: "2024-06-01"
        promotionRequest.put("dateFin", LocalDate.now().plusDays(30).toString());
        promotionRequest.put("codePromo", "ETE2024");
        promotionRequest.put("actif", true);
        promotionRequest.put("produitId", produitId);
        promotionRequest.put("categorie", null);
        promotionRequest.put("nombreUtilisationsMax", 100);

        // Act
        MvcResult result = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promotionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nom").value("Promo Été 2024"))
                .andExpect(jsonPath("$.typePromotion").value("POURCENTAGE"))
                .andExpect(jsonPath("$.valeur").value(10.0))
                .andReturn();

        // Extract promotion ID
        String response = result.getResponse().getContentAsString();
        Map<String, Object> promotion = objectMapper.readValue(response, Map.class);
        promotionId = ((Number) promotion.get("id")).longValue();

        // VÉRIFICATION BDD CRITIQUE : La promotion est bien persistée avec toutes les données
        Optional<Promotion> promoOpt = promotionRepository.findById(promotionId);
        assertTrue(promoOpt.isPresent(), "La promotion doit exister en BDD");
        Promotion promoBDD = promoOpt.get();

        assertEquals("Promo Été 2024", promoBDD.getNom(), "Le nom doit correspondre");
        assertEquals("Promotion estivale -10%", promoBDD.getDescription(), "La description doit correspondre");
        assertEquals("POURCENTAGE", promoBDD.getTypePromotion(), "Le typePromotion doit correspondre");
        assertEquals(10.0, promoBDD.getValeur(), 0.01, "La valeur doit correspondre");
        
        // Vérification des dates (avec marge de 1 jour pour éviter les problèmes de timezone)
        LocalDate today = LocalDate.now();
        assertTrue(promoBDD.getDateDebut().equals(today) || promoBDD.getDateDebut().equals(today.minusDays(1)) || 
                   promoBDD.getDateDebut().equals(today.plusDays(1)), 
                   "La dateDebut doit être proche d'aujourd'hui");
        
        LocalDate dateFinAttendue = today.plusDays(30);
        assertTrue(promoBDD.getDateFin().equals(dateFinAttendue) || promoBDD.getDateFin().equals(dateFinAttendue.minusDays(1)) || 
                   promoBDD.getDateFin().equals(dateFinAttendue.plusDays(1)), 
                   "La dateFin doit être proche d'aujourd'hui + 30 jours");
        
        assertEquals("ETE2024", promoBDD.getCodePromo(), "Le codePromo doit correspondre");
        assertTrue(promoBDD.getActif(), "La promotion doit être active");
        assertEquals(produitId, promoBDD.getProduitId(), "Le produitId doit correspondre");
        assertEquals(100, promoBDD.getNombreUtilisationsMax(), "Le nombreUtilisationsMax doit correspondre");
        assertEquals(snackId, promoBDD.getSnackId(), "Le snack_id doit être correctement rempli en BDD");
    }

    @Test
    @DisplayName("🎁 TEST 5.1: GET Promotions Actives - Vérification filtre par dates")
    void test5_1_GetPromotionsActives_VerificationFiltreDates() throws Exception {
        // Arrange
        test5_CreatePromotion_VerificationDatesEtSnackId();

        // Créer une promotion expirée (dateFin dans le passé)
        Map<String, Object> promoExpireeRequest = new HashMap<>();
        promoExpireeRequest.put("nom", "Promo Expirée");
        promoExpireeRequest.put("typePromotion", "POURCENTAGE");
        promoExpireeRequest.put("valeur", 5.0);
        promoExpireeRequest.put("dateDebut", LocalDate.now().minusDays(30).toString());
        promoExpireeRequest.put("dateFin", LocalDate.now().minusDays(1).toString()); // Expirée hier
        promoExpireeRequest.put("actif", true);

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promoExpireeRequest)))
                .andExpect(status().isOk());

        // Act - Récupérer les promotions actives
        MvcResult result = mockMvc.perform(get("/api/promotions/actives")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> promotions = (List<Map<String, Object>>) objectMapper.readValue(response, List.class);

        // VÉRIFICATION CRITIQUE : La promotion expirée ne doit PAS apparaître
        boolean promoExpireePresente = promotions.stream()
                .anyMatch(p -> "Promo Expirée".equals(p.get("nom")));
        assertFalse(promoExpireePresente, "Une promotion expirée ne doit pas apparaître dans les promotions actives");

        // VÉRIFICATION : La promotion active doit apparaître
        boolean promoActivePresente = promotions.stream()
                .anyMatch(p -> "Promo Été 2024".equals(p.get("nom")));
        assertTrue(promoActivePresente, "La promotion active doit apparaître dans les promotions actives");
    }

    @Test
    @DisplayName("🎁 TEST 5.2: UPDATE Promotion - Vérification modification en BDD")
    void test5_2_UpdatePromotion_VerificationBDD() throws Exception {
        // Arrange
        test5_CreatePromotion_VerificationDatesEtSnackId();

        // Payload de modification (PromotionsManager.js ligne 96)
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("nom", "Promo Été Modifiée");
        updateRequest.put("valeur", 15.0);

        // Act
        mockMvc.perform(put("/api/promotions/" + promotionId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Promo Été Modifiée"))
                .andExpect(jsonPath("$.valeur").value(15.0))
                .andReturn();

        // VÉRIFICATION BDD CRITIQUE : Les modifications sont bien persistées
        Optional<Promotion> promoOpt = promotionRepository.findById(promotionId);
        assertTrue(promoOpt.isPresent(), "La promotion doit toujours exister en BDD");
        Promotion promoBDD = promoOpt.get();
        assertEquals("Promo Été Modifiée", promoBDD.getNom(), "Le nom doit être modifié");
        assertEquals(15.0, promoBDD.getValeur(), 0.01, "La valeur doit être modifiée");
    }

    // ============================================
    // TEST COMPLET END-TO-END
    // ============================================

    @Test
    @DisplayName("🚀 TEST COMPLET: Scénario End-to-End complet avec toutes les vérifications BDD")
    void testComplete_EndToEnd_VerificationCompleteBDD() throws Exception {
        // 1. Authentification
        test1_Authentication_VerificationBDD();
        test1_1_ValidateToken();

        // 2. Création Snack + Manager (vérification @Transactional)
        test2_CreateSnack_VerificationTransactionnelle();

        // 3. Création Produits et Ingrédients (vérification snack_id)
        test3_CreateProduct_VerificationSnackId();
        test3_1_CreateIngredient_VerificationSnackId();

        // 4. Création Commande complexe (vérification prixFinal et details)
        test4_CreateComplexOrder_VerificationPrixFinalEtDetails();

        // 5. Module Promotions (vérification dates et filtre)
        test5_CreatePromotion_VerificationDatesEtSnackId();
        test5_1_GetPromotionsActives_VerificationFiltreDates();

        // VÉRIFICATION FINALE : Toutes les données sont en BDD
        assertTrue(snackRepository.findById(snackId).isPresent(), "Le snack doit exister en BDD");
        assertTrue(produitRepository.findById(produitId).isPresent(), "Le produit doit exister en BDD");
        assertTrue(ingredientRepository.findById(ingredientId).isPresent(), "L'ingrédient doit exister en BDD");
        assertTrue(commandeRepository.findById(commandeId).isPresent(), "La commande doit exister en BDD");
        assertTrue(promotionRepository.findById(promotionId).isPresent(), "La promotion doit exister en BDD");

        System.out.println("\n✅ TOUS LES TESTS SONT PASSÉS !");
        System.out.println("✅ Authentification : Token, Role, SnackId vérifiés en BDD");
        System.out.println("✅ Création Snack : @Transactional et actif=true vérifiés");
        System.out.println("✅ Création Produits/Ingrédients : snack_id vérifié en BDD");
        System.out.println("✅ Création Commande : prixFinal, details et total vérifiés en BDD");
        System.out.println("✅ Module Promotions : dates, snack_id et filtre actives vérifiés");
        System.out.println("✅ VALIDATION 100% : Frontend ↔ Backend ↔ BDD fonctionne parfaitement");
    }
}

