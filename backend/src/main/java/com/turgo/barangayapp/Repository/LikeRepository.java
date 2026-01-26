package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    List<Like> findByEntityTypeAndEntityId(String entityType, Long entityId);
    Optional<Like> findByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    long countByEntityTypeAndEntityId(String entityType, Long entityId);
}
