package com.gideon.notes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideon.notes.dto.NoteDto;
import com.gideon.notes.security.JwtService;
import com.gideon.notes.service.notes.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotesController.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NoteService noteService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private NoteDto.NoteRequest noteRequest;
    private NoteDto.NoteResponse noteResponse;

    @BeforeEach
    void setUp() {
        noteRequest = NoteDto.NoteRequest.builder()
                .title("Test Note")
                .content("Test content")
                .tags(Arrays.asList("test", "sample"))
                .build();

        noteResponse = NoteDto.NoteResponse.builder()
                .id(1L)
                .title("Test Note")
                .content("Test content")
                .tags(Arrays.asList("test", "sample"))
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(new User("testuser", "password", new ArrayList<>()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createNote_ShouldReturnCreatedNote() throws Exception {
        when(noteService.createNote(eq("testuser"), any(NoteDto.NoteRequest.class)))
                .thenReturn(noteResponse);

        mockMvc.perform(post("/api/notes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("Test content"))
                .andExpect(jsonPath("$.tags[0]").value("test"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createNote_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        NoteDto.NoteRequest invalidRequest = NoteDto.NoteRequest.builder()
                .title("")
                .content("Test content")
                .build();

        mockMvc.perform(post("/api/notes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getNoteById_ShouldReturnNote() throws Exception {
        when(noteService.getNoteById(eq("testuser"), eq(1L)))
                .thenReturn(noteResponse);

        mockMvc.perform(get("/api/notes/1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Note"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateNote_WithIfMatch_ShouldReturnUpdatedNote() throws Exception {
        NoteDto.NoteResponse updatedResponse = NoteDto.NoteResponse.builder()
                .id(1L)
                .title("Updated Note")
                .content("Updated content")
                .tags(Arrays.asList("updated"))
                .version(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(noteService.updateNote(eq("testuser"), eq(1L), any(NoteDto.NoteRequest.class), eq(0L)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/notes/1")
                        .with(csrf())
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteNote_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notes/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void createNote_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isUnauthorized());
    }
}
