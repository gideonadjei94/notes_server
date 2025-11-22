package com.gideon.notes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideon.notes.dto.AuthDto;
import com.gideon.notes.dto.NoteDto;
import com.gideon.notes.entity.Note;
import com.gideon.notes.entity.User;
import com.gideon.notes.enums.UserDomain;
import com.gideon.notes.repository.NotesRepository;
import com.gideon.notes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotesRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String jwtToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up database
        noteRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("integrationtest")
                .email("test@notes.com")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserDomain.USER)
                .build();
        testUser = userRepository.save(testUser);

        // Login to get JWT token
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("test@notes.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthDto.AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthDto.AuthResponse.class
        );
        jwtToken = authResponse.getToken();
    }


    @Test
    void fullNoteLifecycle_CreateReadUpdateDelete() throws Exception {
        // Create note
        NoteDto.NoteRequest createRequest = NoteDto.NoteRequest.builder()
                .title("Integration Test Note")
                .content("This is a test note for integration testing")
                .tags(Arrays.asList("integration", "test"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Note"))
                .andExpect(jsonPath("$.tags[0]").value("integration"))
                .andReturn();

        NoteDto.NoteResponse createdNote = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                NoteDto.NoteResponse.class
        );

        // Read note
        mockMvc.perform(get("/api/notes/" + createdNote.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdNote.getId()))
                .andExpect(jsonPath("$.title").value("Integration Test Note"));

        // Update note
        NoteDto.NoteRequest updateRequest = NoteDto.NoteRequest.builder()
                .title("Updated Integration Test Note")
                .content("Updated content")
                .tags(Arrays.asList("updated"))
                .build();

        mockMvc.perform(put("/api/notes/" + createdNote.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("If-Match", createdNote.getVersion().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Integration Test Note"))
                .andExpect(jsonPath("$.version").value(1));

        // Soft delete note
        mockMvc.perform(delete("/api/notes/" + createdNote.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify note is soft deleted (not visible in normal queries)
        mockMvc.perform(get("/api/notes/" + createdNote.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());

        // Restore note
        mockMvc.perform(post("/api/notes/" + createdNote.getId() + "/restore")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());
    }


    @Test
    void searchAndPagination_ShouldWorkCorrectly() throws Exception {
        // Create multiple notes
        for (int i = 1; i <= 15; i++) {
            Note note = Note.builder()
                    .title("Note " + i)
                    .content("Content for note " + i + (i % 2 == 0 ? " searchable" : ""))
                    .user(testUser)
                    .build();

            if (i % 3 == 0) {
                note.setTagList(Arrays.asList("important", "work"));
            } else if (i % 2 == 0) {
                note.setTagList(Arrays.asList("personal"));
            }

            noteRepository.save(note);
        }

        // Test pagination
        MvcResult page1Result = mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.notes").isArray())
                .andReturn();

        NoteDto.PagedNotesResponse page1 = objectMapper.readValue(
                page1Result.getResponse().getContentAsString(),
                NoteDto.PagedNotesResponse.class
        );
        assertThat(page1.getNotes()).hasSize(10);

        // Test page 2
        MvcResult page2Result = mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").isArray())
                .andReturn();

        NoteDto.PagedNotesResponse page2 = objectMapper.readValue(
                page2Result.getResponse().getContentAsString(),
                NoteDto.PagedNotesResponse.class
        );
        assertThat(page2.getNotes()).hasSize(5);

        // Test search
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("search", "searchable")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(7));

        // Test tag filter
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("tag", "important")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));

        // Test combined search and tag filter
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("search", "searchable")
                        .param("tag", "important")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // Note 6, 12

        // Test sorting
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("sortBy", "title")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }


    @Test
    void optimisticLocking_ShouldPreventConcurrentUpdates() throws Exception {
        // Create a note
        NoteDto.NoteRequest createRequest = NoteDto.NoteRequest.builder()
                .title("Concurrency Test")
                .content("Original content")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        NoteDto.NoteResponse createdNote = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                NoteDto.NoteResponse.class
        );

        NoteDto.NoteRequest updateRequest1 = NoteDto.NoteRequest.builder()
                .title("First Update")
                .content("Updated by user 1")
                .build();

        mockMvc.perform(put("/api/notes/" + createdNote.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        NoteDto.NoteRequest updateRequest2 = NoteDto.NoteRequest.builder()
                .title("Second Update")
                .content("Updated by user 2 with stale version")
                .build();

        mockMvc.perform(put("/api/notes/" + createdNote.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("If-Match", "0") // Old version
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest2)))
                .andExpect(status().isConflict());
    }


    @Test
    void signup_ShouldCreateNewUser() throws Exception {
        AuthDto.SignupRequest signupRequest = AuthDto.SignupRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));

        assertThat(userRepository.findByUsername("newuser")).isPresent();
    }
}
