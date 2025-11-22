package com.gideon.notes.service.notes;

import com.gideon.notes.dto.NoteDto;

public interface NoteServiceInt {
    NoteDto.NoteResponse createNote(String email, NoteDto.NoteRequest request);
    NoteDto.PagedNotesResponse getNotes(String email, String search, String tag, int page, int size, String sortBy);
    NoteDto.NoteResponse getNoteById(String email, Long id);
    NoteDto.NoteResponse updateNote(String email, Long id, NoteDto.NoteRequest request, Long version);
    void deleteNote(String username, Long id);
    NoteDto.NoteResponse restoreNote(String email, Long id);
}
