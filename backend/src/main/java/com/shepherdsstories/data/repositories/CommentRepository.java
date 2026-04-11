package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findAllByPostId(UUID postId);

    List<Comment> findAllByParentComment(Comment parentComment);
}