package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.CommentLike;
import com.shepherdsstories.entities.CommentLikeId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {
    long countByCommentId(UUID commentId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT cl FROM CommentLike cl " +
            "LEFT JOIN FETCH cl.user u " +
            "WHERE cl.comment.id = :commentId " +
            "ORDER BY cl.createdAt DESC")
    List<CommentLike> findLatestLikes(UUID commentId, Pageable pageable);
}
