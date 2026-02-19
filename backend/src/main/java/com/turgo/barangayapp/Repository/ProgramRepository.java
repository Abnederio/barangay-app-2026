package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Announcement;
import com.turgo.barangayapp.Model.Program;
import com.turgo.barangayapp.enums.AnnouncementCategory;
import com.turgo.barangayapp.enums.EventCategory;
import com.turgo.barangayapp.enums.ProgramCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByEndDateAfterOrderByStartDateAsc(LocalDateTime now);
    List<Program> findByProgramCategory(ProgramCategory category);
}
