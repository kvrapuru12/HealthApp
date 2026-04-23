package com.healthapp.controller;

import com.healthapp.entity.SleepEntry;
import com.healthapp.entity.StepEntry;
import com.healthapp.entity.User;
import com.healthapp.entity.WaterEntry;
import com.healthapp.repository.SleepEntryRepository;
import com.healthapp.repository.StepEntryRepository;
import com.healthapp.repository.UserRepository;
import com.healthapp.repository.WaterEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TimezoneEndpointsIntegrationTest {

    private static final DateTimeFormatter UTC_Z =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WaterEntryRepository waterEntryRepository;

    @Autowired
    private StepEntryRepository stepEntryRepository;

    @Autowired
    private SleepEntryRepository sleepEntryRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("timezone-user");
        user.setEmail("timezone-user@example.com");
        user.setPassword("password");
        user = userRepository.save(user);
    }

    @Test
    void waterCreate_acceptsUtcZTimestamp() throws Exception {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String body = """
                {
                  "userId": %d,
                  "amount": 500,
                  "note": "",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts));

        mockMvc.perform(post("/water")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        List<WaterEntry> saved = waterEntryRepository.findByUserIdAndStatus(user.getId(), WaterEntry.Status.ACTIVE);
        assertEquals(1, saved.size());
        assertEquals(ts.toLocalDateTime(), saved.get(0).getLoggedAt());
    }

    @Test
    void waterPatch_acceptsUtcZTimestamp() throws Exception {
        WaterEntry entry = new WaterEntry(user, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(3), 300, "before");
        entry = waterEntryRepository.save(entry);
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String body = """
                {
                  "loggedAt": "%s"
                }
                """.formatted(formatUtcZ(ts));

        mockMvc.perform(patch("/water/{id}", entry.getId())
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        WaterEntry updated = waterEntryRepository.findByIdAndStatus(entry.getId(), WaterEntry.Status.ACTIVE).orElse(null);
        assertNotNull(updated);
        assertEquals(ts.toLocalDateTime(), updated.getLoggedAt());
    }

    @Test
    void stepsCreate_acceptsUtcZTimestamp() throws Exception {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String body = """
                {
                  "userId": %d,
                  "stepCount": 8200,
                  "note": "test",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts));

        mockMvc.perform(post("/steps")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        List<StepEntry> saved = stepEntryRepository.findByUserIdAndStatus(user.getId(), StepEntry.Status.ACTIVE);
        assertEquals(1, saved.size());
        assertEquals(ts.toLocalDateTime(), saved.get(0).getLoggedAt());
    }

    @Test
    void stepsPatch_acceptsUtcZTimestamp() throws Exception {
        StepEntry entry = new StepEntry(user, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(6), 4000, "before");
        entry = stepEntryRepository.save(entry);
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String body = """
                {
                  "loggedAt": "%s"
                }
                """.formatted(formatUtcZ(ts));

        mockMvc.perform(patch("/steps/{id}", entry.getId())
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        StepEntry updated = stepEntryRepository.findByIdAndStatus(entry.getId(), StepEntry.Status.ACTIVE).orElse(null);
        assertNotNull(updated);
        assertEquals(ts.toLocalDateTime(), updated.getLoggedAt());
    }

    @Test
    void sleepCreate_acceptsUtcZTimestamp() throws Exception {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String body = """
                {
                  "userId": %d,
                  "hours": 7.5,
                  "note": "test",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts));

        mockMvc.perform(post("/sleeps")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        List<SleepEntry> saved = sleepEntryRepository.findByUserIdAndStatus(user.getId(), SleepEntry.Status.ACTIVE);
        assertEquals(1, saved.size());
        assertEquals(ts.toLocalDateTime(), saved.get(0).getLoggedAt());
    }

    @Test
    void stepsCreate_allowsImmediateDifferentStepCount() throws Exception {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String firstBody = """
                {
                  "userId": %d,
                  "stepCount": 6000,
                  "note": "first",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts));

        String secondBody = """
                {
                  "userId": %d,
                  "stepCount": 6500,
                  "note": "second",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts.plusSeconds(20)));

        mockMvc.perform(post("/steps")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/steps")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isCreated());

        List<StepEntry> saved = stepEntryRepository.findByUserIdAndStatus(user.getId(), StepEntry.Status.ACTIVE);
        assertEquals(2, saved.size());
    }

    @Test
    void sleepCreate_allowsImmediateDifferentHours() throws Exception {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).withNano(0);

        String firstBody = """
                {
                  "userId": %d,
                  "hours": 7.5,
                  "note": "first",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts));

        String secondBody = """
                {
                  "userId": %d,
                  "hours": 8.0,
                  "note": "second",
                  "loggedAt": "%s"
                }
                """.formatted(user.getId(), formatUtcZ(ts.plusSeconds(20)));

        mockMvc.perform(post("/sleeps")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/sleeps")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isCreated());

        List<SleepEntry> saved = sleepEntryRepository.findByUserIdAndStatus(user.getId(), SleepEntry.Status.ACTIVE);
        assertEquals(2, saved.size());
    }

    private static Authentication auth(Long userId, boolean admin) {
        List<SimpleGrantedAuthority> roles = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new UsernamePasswordAuthenticationToken(userId, null, roles);
    }

    private static String formatUtcZ(OffsetDateTime value) {
        return value.withOffsetSameInstant(ZoneOffset.UTC).format(UTC_Z);
    }
}
