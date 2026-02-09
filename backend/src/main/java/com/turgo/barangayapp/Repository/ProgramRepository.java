package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByEndDateAfterOrderByStartDateAsc(LocalDateTime now);
}
