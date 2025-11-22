package com.gideon.notes.service.notes;

import com.gideon.notes.dto.NotesDto;

public interface NotesServiceInt {
    NotesDto.NoteResponse createNote(String username, NotesDto.NoteRequest request);
    NotesDto.PagedNotesResponse getNotes(String username, String search, String tag, int page, int size, String sortBy);
    NotesDto.NoteResponse getNoteById(String username, Long id);
    NotesDto.NoteResponse updateNote(String username, Long id, NotesDto.NoteRequest request, Long version);
    void deleteNote(String username, Long id);
    NotesDto.NoteResponse restoreNote(String username, Long id);
}
