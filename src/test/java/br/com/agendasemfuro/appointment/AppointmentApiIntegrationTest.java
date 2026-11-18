package br.com.agendasemfuro.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class AppointmentApiIntegrationTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void bookingLifecycleWorksAcrossHttpAndPersistenceLayers() throws Exception {
        LocalDate appointmentDate = LocalDate.now(BUSINESS_ZONE).plusDays(7);
        LocalDateTime startsAt = appointmentDate.atTime(10, 0);

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        String barberId = createBarber();
        String serviceId = createService();
        createWorkingHours(barberId, appointmentDate.getDayOfWeek().getValue());

        LocalDate pastDate = LocalDate.now(BUSINESS_ZONE).minusDays(7);
        MvcResult pastAvailability = mockMvc.perform(get("/api/barbers/{barberId}/availability", barberId)
                        .param("date", pastDate.toString())
                        .param("serviceId", serviceId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(pastAvailability.getResponse().getContentAsString())
                .get("availableSlots").isEmpty()).isTrue();

        mockMvc.perform(get("/api/barbers/{barberId}/availability", barberId)
                        .param("date", appointmentDate.toString())
                        .param("serviceId", serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").isArray());

        String request = """
                {
                  "barberId": "%s",
                  "serviceId": "%s",
                  "customerName": "Ana Souza",
                  "customerPhone": "11999999999",
                  "startsAt": "%s"
                }
                """.formatted(barberId, serviceId, startsAt);

        MvcResult createResult = mockMvc.perform(post("/api/appointments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.endsAt").value(startsAt.plusMinutes(30).toString() + ":00"))
                .andReturn();
        String appointmentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/appointments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());

        MvcResult availabilityAfterBooking = mockMvc.perform(get("/api/barbers/{barberId}/availability", barberId)
                        .param("date", appointmentDate.toString())
                        .param("serviceId", serviceId))
                .andExpect(status().isOk())
                .andReturn();
        String slotsAfterBooking = objectMapper.readTree(availabilityAfterBooking.getResponse().getContentAsString())
                .get("availableSlots").toString();
        assertThat(slotsAfterBooking).doesNotContain("10:00:00").contains("10:30:00");

        mockMvc.perform(get("/api/appointments/{appointmentId}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Ana Souza"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(patch("/api/appointments/{appointmentId}/cancel", appointmentId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/appointments/{appointmentId}/cancel", appointmentId).with(csrf()))
                .andExpect(status().isConflict());

        MvcResult availabilityAfterCancellation = mockMvc.perform(get("/api/barbers/{barberId}/availability", barberId)
                        .param("date", appointmentDate.toString())
                        .param("serviceId", serviceId))
                .andExpect(status().isOk())
                .andReturn();
        String slotsAfterCancellation = objectMapper
                .readTree(availabilityAfterCancellation.getResponse().getContentAsString())
                .get("availableSlots").toString();
        assertThat(slotsAfterCancellation).contains("10:00:00");

        mockMvc.perform(post("/api/appointments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
    }

    private String createBarber() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/barbers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Barbeiro de integração"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private String createService() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/services")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Corte de integração","durationMinutes":30,"price":45.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private void createWorkingHours(String barberId, int dayOfWeek) throws Exception {
        mockMvc.perform(post("/api/barbers/{barberId}/working-hours", barberId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":%d,"opensAt":"09:00:00","closesAt":"18:00:00"}
                                """.formatted(dayOfWeek)))
                .andExpect(status().isCreated());
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = response.get("id").asText();
        assertThat(id).isNotBlank();
        return id;
    }
}
