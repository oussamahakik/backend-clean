package caisse.manager.caisse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommandeHistoryRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void historyEndpointWorksWithDateFilter() throws Exception {
        String superAdminToken = login("hakik_owner", "oussamahakikOwner");

        String suffix = String.valueOf(System.currentTimeMillis());
        String managerUsername = "manager_hist_" + suffix;
        String managerPassword = "password123";

        Map<String, String> createSnack = new HashMap<>();
        createSnack.put("nomRestaurant", "Snack Hist " + suffix);
        createSnack.put("adresse", "Rue Hist");
        createSnack.put("usernameManager", managerUsername);
        createSnack.put("passwordManager", managerPassword);

        MvcResult createSnackResult = mockMvc.perform(post("/api/super-admin/snacks")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSnack)))
                .andExpect(status().isOk())
                .andReturn();

        Matcher matcher = Pattern.compile("ID: (\\d+)").matcher(createSnackResult.getResponse().getContentAsString());
        assertTrue(matcher.find());
        Long snackId = Long.valueOf(matcher.group(1));

        String managerToken = login(managerUsername, managerPassword);
        assertNotNull(managerToken);

        mockMvc.perform(get("/api/commandes/history")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("X-Snack-ID", snackId.toString())
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        Map<String, String> loginPayload = new HashMap<>();
        loginPayload.put("username", username);
        loginPayload.put("password", password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        assertEquals(username, response.get("username"));
        return (String) response.get("token");
    }
}
