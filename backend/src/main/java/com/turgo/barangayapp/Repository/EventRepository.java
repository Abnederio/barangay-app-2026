package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventDateAfterOrderByEventDateAsc(LocalDateTime date);
    List<Event> findAllByOrderByEventDateDesc();
}
