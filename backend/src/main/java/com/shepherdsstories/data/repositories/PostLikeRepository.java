package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.PostLike;
import com.shepherdsstories.entities.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    long countByPostId(UUID postId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
}
