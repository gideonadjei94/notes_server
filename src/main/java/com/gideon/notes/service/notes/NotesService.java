package com.gideon.notes.service.notes;


import com.gideon.notes.dto.NotesDto;
import com.gideon.notes.entity.Notes;
import com.gideon.notes.entity.User;
import com.gideon.notes.exception.EntityNotFoundException;
import com.gideon.notes.exception.VersionConflictException;
import com.gideon.notes.repository.NotesRepository;
import com.gideon.notes.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotesService {

    private final NotesRepository noteRepo;
    private final UserRepository userRepo;

    @Transactional
    public NotesDto.NoteResponse createNote(String username, NotesDto.NoteRequest request) {
        User user = getUserByUsername(username);

        Notes note = Notes.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .user(user)
                .build();

        note.setTagList(request.getTags());
        note = noteRepo.save(note);

        return toNoteResponse(note);
    }


    @Transactional(readOnly = true)
    public NotesDto.PagedNotesResponse getNotes(String username,
                                               String search,
                                               String tag,
                                               int page,
                                               int size,
                                               String sortBy) {
        User user = getUserByUsername(username);

        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "updatedAt";
        }

        Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Notes> notePage;

        // Apply filters
        if (search != null && !search.trim().isEmpty() && tag != null && !tag.trim().isEmpty()) {
            notePage = noteRepo.searchNotesWithTag(user.getId(), search.trim(), tag.trim().toLowerCase(), pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            notePage = noteRepo.searchNotes(user.getId(), search.trim(), pageable);
        } else if (tag != null && !tag.trim().isEmpty()) {
            notePage = noteRepo.findByUserIdAndTag(user.getId(), tag.trim().toLowerCase(), pageable);
        } else {
            notePage = noteRepo.findByUserId(user.getId(), pageable);
        }

        List<NotesDto.NoteResponse> notes = notePage.getContent().stream()
                .map(this::toNoteResponse)
                .collect(Collectors.toList());

        return NotesDto.PagedNotesResponse.builder()
                .notes(notes)
                .page(notePage.getNumber())
                .size(notePage.getSize())
                .totalElements(notePage.getTotalElements())
                .totalPages(notePage.getTotalPages())
                .last(notePage.isLast())
                .build();
    }


    @Transactional(readOnly = true)
    public NotesDto.NoteResponse getNoteById(String username, Long id) {
        User user = getUserByUsername(username);
        Notes note = noteRepo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Note not found with id: " + id));

        return toNoteResponse(note);
    }


    @Transactional
    public NotesDto.NoteResponse updateNote(String username, Long id, NotesDto.NoteRequest request, Long version) {
        User user = getUserByUsername(username);
        Notes note = noteRepo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Note not found with id: " + id));

        if (version != null && !note.getVersion().equals(version)) {
            throw new VersionConflictException("Note was modified by another user");
        }

        note.setTitle(request.getTitle().trim());
        note.setContent(request.getContent().trim());
        note.setTagList(request.getTags());

        try {
            note = noteRepo.save(note);
        } catch (OptimisticLockException e) {
            throw new VersionConflictException("Note was modified by another user");
        }

        return toNoteResponse(note);
    }


    @Transactional
    public void deleteNote(String username, Long id) {
        User user = getUserByUsername(username);
        Notes note = noteRepo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Note not found with id: " + id));

        note.softDelete();
        noteRepo.save(note);
    }


    @Transactional
    public NotesDto.NoteResponse restoreNote(String username, Long id) {
        User user = getUserByUsername(username);

        Notes note = noteRepo.findById(id)
                .filter(n -> n.getUser().getId().equals(user.getId()) && n.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Deleted note not found with id: " + id));

        note.restore();
        note = noteRepo.save(note);

        return toNoteResponse(note);
    }


    private User getUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }



    private NotesDto.NoteResponse toNoteResponse(Notes note) {
        return NotesDto.NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .tags(note.getTagList())
                .version(note.getVersion())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .deletedAt(note.getDeletedAt())
                .build();
    }
}