package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query("SELECT DISTINCT p FROM Post p " +
            "JOIN FETCH p.author a " +
            "JOIN ConnectionRequest cr ON cr.missionary = a " +
            "JOIN cr.supporter s " +
            "WHERE s.id = :supporterId " +
            "AND cr.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<Post> findAllForSupporter(@Param("supporterId") UUID supporterId, @Param("status") RequestStatus status);

    @Query("SELECT p FROM Post p JOIN FETCH p.author a WHERE a.id = :authorId ORDER BY p.createdAt DESC")
    List<Post> findAllByAuthorIdWithAuthor(@Param("authorId") UUID authorId);
}