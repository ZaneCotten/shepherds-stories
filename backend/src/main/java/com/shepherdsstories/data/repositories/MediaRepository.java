package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {
    // Find all media for a specific post
    List<Media> findAllByPostId(UUID postId);

    // This will automatically sort by your order_number field ascending (0, 1, 2...)
    List<Media> findAllByPostIdOrderByOrderNumberAsc(UUID postId);
}
