package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);
}
