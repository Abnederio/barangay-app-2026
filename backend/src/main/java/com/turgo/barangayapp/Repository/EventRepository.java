package com.turgo.barangayapp.Repository;

import com.turgo.barangayapp.Model.Announcement;
import com.turgo.barangayapp.Model.Event;
import com.turgo.barangayapp.enums.AnnouncementCategory;
import com.turgo.barangayapp.enums.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventDateAfterOrderByEventDateAsc(LocalDateTime now);
    List<Event> findByEventCategory(EventCategory category);
}
