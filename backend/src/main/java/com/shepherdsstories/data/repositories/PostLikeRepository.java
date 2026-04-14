package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.PostLike;
import com.shepherdsstories.entities.PostLikeId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    long countByPostId(UUID postId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    @Query("SELECT pl FROM PostLike pl " +
            "LEFT JOIN FETCH pl.user u " +
            "WHERE pl.post.id = :postId " +
            "ORDER BY pl.createdAt DESC")
    List<PostLike> findLatestLikes(UUID postId, Pageable pageable);
}
