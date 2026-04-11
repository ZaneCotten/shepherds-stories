package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    // Get all posts from a specific missionary sorted by published date (newest first)
    List<Post> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    // Get all posts sorted by published date (newest first)
    List<Post> findAllByOrderByCreatedAtDesc();
}