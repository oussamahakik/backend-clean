package caisse.manager.caisse.cucumber;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class CaisseStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("ID: (\\d+)");

    private final Map<String, String> superAdminCredentials = new HashMap<>();
    private String superAdminToken;
    private String managerToken;
    private Long snackId;
    private MvcResult lastResult;

    @Etantdonné("les credentials du super admin sont valides")
    public void superAdminCredentialsSontValides() {
        superAdminCredentials.put("username", "hakik_owner");
        superAdminCredentials.put("password", "oussamahakikOwner");
    }

    @Quand("le super admin se connecte")
    public void leSuperAdminSeConnecte() throws Exception {
        lastResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(superAdminCredentials)))
                .andReturn();
    }

    @Alors("la connexion super admin réussit")
    public void laConnexionSuperAdminReussit() throws Exception {
        Assertions.assertEquals(200, lastResult.getResponse().getStatus());
        Map<String, Object> payload = objectMapper.readValue(
                lastResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        Assertions.assertTrue(payload.containsKey("token"));
        superAdminToken = (String) payload.get("token");
        Assertions.assertNotNull(superAdminToken);
    }

    @Etantdonné("un super admin authentifié")
    public void unSuperAdminAuthentifie() throws Exception {
        superAdminCredentialsSontValides();
        leSuperAdminSeConnecte();
        laConnexionSuperAdminReussit();
    }

    @Quand("le super admin crée un snack avec manager")
    public void leSuperAdminCreeUnSnackAvecManager() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String managerUsername = "manager_cuk_" + suffix;
        String managerPassword = "password123";

        Map<String, String> payload = new HashMap<>();
        payload.put("nomRestaurant", "Snack Cucumber " + suffix);
        payload.put("adresse", "Rue Cucumber");
        payload.put("usernameManager", managerUsername);
        payload.put("passwordManager", managerPassword);

        lastResult = mockMvc.perform(post("/api/super-admin/snacks")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();

        Matcher matcher = ID_PATTERN.matcher(lastResult.getResponse().getContentAsString());
        Assertions.assertTrue(matcher.find(), "Réponse création snack invalide: id absent");
        snackId = Long.valueOf(matcher.group(1));

        Map<String, String> managerLoginPayload = new HashMap<>();
        managerLoginPayload.put("username", managerUsername);
        managerLoginPayload.put("password", managerPassword);
        MvcResult managerLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLoginPayload)))
                .andReturn();

        Assertions.assertEquals(200, managerLoginResult.getResponse().getStatus());
        Map<String, Object> managerAuth = objectMapper.readValue(
                managerLoginResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        managerToken = (String) managerAuth.get("token");
    }

    @Alors("le snack et le manager sont utilisables")
    public void leSnackEtLeManagerSontUtilisables() {
        Assertions.assertEquals(200, lastResult.getResponse().getStatus());
        Assertions.assertNotNull(snackId);
        Assertions.assertNotNull(managerToken);
    }

    @Etantdonné("un manager authentifié")
    public void unManagerAuthentifie() throws Exception {
        unSuperAdminAuthentifie();
        leSuperAdminCreeUnSnackAvecManager();
        leSnackEtLeManagerSontUtilisables();
    }

    @Quand("le manager consulte l'historique des commandes du jour")
    public void leManagerConsulteHistoriqueCommandes() throws Exception {
        String today = LocalDate.now().toString();
        lastResult = mockMvc.perform(get("/api/commandes/history")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .param("date", today))
                .andReturn();
    }

    @Alors("l'historique des commandes est retourné")
    public void historiqueCommandesRetourne() {
        Assertions.assertEquals(200, lastResult.getResponse().getStatus());
        String contentType = lastResult.getResponse().getContentType();
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(contentType.contains("application/json"));
    }
}
