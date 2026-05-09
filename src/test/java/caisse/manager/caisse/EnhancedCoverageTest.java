package caisse.manager.caisse;

import caisse.manager.caisse.model.Imprimante;
import caisse.manager.caisse.model.LogEntry;
import caisse.manager.caisse.model.Plan;
import caisse.manager.caisse.model.Snack;
import caisse.manager.caisse.model.Utilisateur;
import caisse.manager.caisse.repository.ImprimanteRepository;
import caisse.manager.caisse.repository.LogEntryRepository;
import caisse.manager.caisse.repository.PlanRepository;
import caisse.manager.caisse.repository.SnackRepository;
import caisse.manager.caisse.repository.UtilisateurRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Coverage étendue backend - contrôleurs et scénarios métier")
class EnhancedCoverageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SnackRepository snackRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ImprimanteRepository imprimanteRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Test
    @DisplayName("SUPER_ADMIN - endpoint users/count retourne un total valide")
    void usersCountEndpointShouldReturnValidCount() throws Exception {
        String superAdminToken = authenticate("hakik_owner", "oussamahakikOwner");

        mockMvc.perform(get("/api/super-admin/users/count")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    @DisplayName("Abonnement - attribuer et retirer un plan pour un snack")
    void shouldAssignAndClearPlanOnSnack() throws Exception {
        String superAdminToken = authenticate("hakik_owner", "oussamahakikOwner");
        ManagerContext managerContext = createSnackWithManager(superAdminToken);

        Plan plan = new Plan();
        plan.setNom("Plan-Coverage-" + System.nanoTime());
        plan.setPrixMensuel(49.99);
        plan.setActif(true);
        plan = planRepository.save(plan);

        Map<String, Object> assignPayload = new HashMap<>();
        assignPayload.put("planId", plan.getId());
        assignPayload.put("dateFinAbonnement", LocalDate.now().plusDays(30).toString());

        mockMvc.perform(put("/api/super-admin/snacks/" + managerContext.snackId + "/abonnement")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignPayload)))
                .andExpect(status().isOk());

        Snack assignedSnack = snackRepository.findById(managerContext.snackId).orElseThrow();
        assertNotNull(assignedSnack.getPlan());
        assertEquals(plan.getId(), assignedSnack.getPlan().getId());
        assertNotNull(assignedSnack.getDateFinAbonnement());

        Map<String, Object> clearPayload = new HashMap<>();
        clearPayload.put("planId", null);
        clearPayload.put("dateFinAbonnement", "");

        mockMvc.perform(put("/api/super-admin/snacks/" + managerContext.snackId + "/abonnement")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clearPayload)))
                .andExpect(status().isOk());

        Snack clearedSnack = snackRepository.findById(managerContext.snackId).orElseThrow();
        assertNull(clearedSnack.getPlan());
        assertNull(clearedSnack.getDateFinAbonnement());
    }

    @Test
    @DisplayName("Imprimantes - CRUD complet manager + isolation snack")
    void printersCrudAndIsolationShouldWork() throws Exception {
        String superAdminToken = authenticate("hakik_owner", "oussamahakikOwner");
        ManagerContext manager = createSnackWithManager(superAdminToken);

        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("nom", "Printer Coverage");
        createPayload.put("type", "USB");
        createPayload.put("chemin", "HP-USB-01");
        createPayload.put("actif", true);

        MvcResult createResult = mockMvc.perform(post("/api/imprimantes")
                        .header("Authorization", "Bearer " + manager.token)
                        .header("X-Snack-ID", manager.snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        Long printerId = ((Number) created.get("id")).longValue();
        assertTrue(imprimanteRepository.findById(printerId).isPresent());

        mockMvc.perform(put("/api/imprimantes/" + printerId)
                        .header("Authorization", "Bearer " + manager.token)
                        .header("X-Snack-ID", manager.snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Printer Coverage Updated\"}"))
                .andExpect(status().isOk());

        Optional<Imprimante> updated = imprimanteRepository.findById(printerId);
        assertTrue(updated.isPresent());
        assertEquals("Printer Coverage Updated", updated.get().getNom());

        mockMvc.perform(put("/api/imprimantes/" + printerId)
                        .header("Authorization", "Bearer " + manager.token)
                        .header("X-Snack-ID", "999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Should Fail\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/imprimantes/" + printerId + "/test")
                        .header("Authorization", "Bearer " + manager.token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/imprimantes/" + printerId)
                        .header("Authorization", "Bearer " + manager.token))
                .andExpect(status().isOk());
        assertFalse(imprimanteRepository.findById(printerId).isPresent());
    }

    @Test
    @DisplayName("Logs - filtres combinés + format date invalide")
    void logsFilteringShouldSupportCombinedFiltersAndValidateDates() throws Exception {
        String superAdminToken = authenticate("hakik_owner", "oussamahakikOwner");
        ManagerContext manager = createSnackWithManager(superAdminToken);

        LocalDateTime fixedTodayTimestamp = LocalDate.now().atTime(12, 0);

        LogEntry match = new LogEntry();
        match.setTimestamp(fixedTodayTimestamp);
        match.setLevel("ERROR");
        match.setCategory("AUTH");
        match.setMessage("auth failed");
        match.setSnackId(manager.snackId);
        logEntryRepository.save(match);

        LogEntry other = new LogEntry();
        other.setTimestamp(fixedTodayTimestamp);
        other.setLevel("INFO");
        other.setCategory("SYSTEM");
        other.setMessage("system info");
        other.setSnackId(manager.snackId);
        logEntryRepository.save(other);

        MvcResult result = mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .param("snackId", manager.snackId.toString())
                        .param("level", "error")
                        .param("category", "auth")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> logs = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertEquals(1, logs.size());
        assertEquals("auth failed", logs.get(0).get("message"));

        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .param("startDate", "not-a-date")
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sous-users - role normalisé et création manager autorisée")
    void subUsersCreationShouldNormalizeRole() throws Exception {
        String superAdminToken = authenticate("hakik_owner", "oussamahakikOwner");
        ManagerContext manager = createSnackWithManager(superAdminToken);

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "cashier_" + System.nanoTime());
        payload.put("password", "password123");
        payload.put("role", "caissier");

        MvcResult createResult = mockMvc.perform(post("/api/utilisateurs")
                        .header("Authorization", "Bearer " + manager.token)
                        .header("X-Snack-ID", manager.snackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        Long userId = ((Number) created.get("id")).longValue();
        Utilisateur user = utilisateurRepository.findById(userId).orElseThrow();
        assertEquals("ROLE_CAISSIER", user.getRole());
        assertEquals(manager.snackId, user.getSnackId());
    }

    private String authenticate(String username, String password) throws Exception {
        Map<String, String> login = new HashMap<>();
        login.put("username", username);
        login.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        return (String) data.get("token");
    }

    private ManagerContext createSnackWithManager(String superAdminToken) throws Exception {
        String unique = String.valueOf(System.nanoTime());
        String managerUsername = "manager_cov_" + unique;
        String managerPassword = "password123";

        Map<String, String> request = new HashMap<>();
        request.put("nomRestaurant", "Snack Coverage " + unique);
        request.put("adresse", "Adresse " + unique);
        request.put("usernameManager", managerUsername);
        request.put("passwordManager", managerPassword);

        MvcResult result = mockMvc.perform(post("/api/super-admin/snacks")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long snackId = extractSnackId(response);
        String managerToken = authenticate(managerUsername, managerPassword);
        return new ManagerContext(snackId, managerToken);
    }

    private Long extractSnackId(String response) {
        int start = response.indexOf("ID: ");
        if (start < 0) {
            throw new IllegalStateException("Impossible d'extraire snackId depuis: " + response);
        }
        int from = start + 4;
        int end = response.indexOf(")", from);
        return Long.parseLong(response.substring(from, end).trim());
    }

    private record ManagerContext(Long snackId, String token) {}
}
