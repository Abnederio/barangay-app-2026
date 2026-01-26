package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.ServiceApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceApplicationRepository extends JpaRepository<ServiceApplication, Long> {
    List<ServiceApplication> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<ServiceApplication> findAllByOrderBySubmittedAtDesc();
}
