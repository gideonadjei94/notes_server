package com.gideon.notes.repository;

import com.gideon.notes.entity.Note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface NotesRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId")
    Page<Note> findByUserId(Long userId, Pageable pageable);

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId " +
            "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Note> searchNotes(@Param("userId") Long userId,
                           @Param("search") String search,
                           Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId " +
            "AND LOWER(n.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    Page<Note> findByUserIdAndTag(@Param("userId") Long userId,
                                  @Param("tag") String tag,
                                  Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId " +
            "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND LOWER(n.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    Page<Note> searchNotesWithTag(@Param("userId") Long userId,
                                  @Param("search") String search,
                                  @Param("tag") String tag,
                                  Pageable pageable);


    @Query(value = "SELECT * FROM notes WHERE id = :id AND user_id = :userId AND deleted_at IS NOT NULL",
            nativeQuery = true)
    Optional<Note> findDeletedNoteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
