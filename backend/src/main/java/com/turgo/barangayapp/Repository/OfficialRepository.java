package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Official;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OfficialRepository extends JpaRepository<Official, Long> {
    List<Official> findByIsActiveTrueOrderByPositionAsc();
}
