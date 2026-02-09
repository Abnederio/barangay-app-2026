package com.turgo.barangayapp.Service;

import com.turgo.barangayapp.Model.Event;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    // --- READ ---
    public List<Event> getUpcomingEvents() {
        return eventRepository.findByEventDateAfterOrderByEventDateAsc(LocalDateTime.now());
    }

    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    // --- CREATE ---
    public Event createEvent(Map<String, String> request, User admin) {
        Event event = new Event();
        event.setTitle(request.get("title"));
        event.setDescription(request.get("description"));
        event.setLocation(request.get("location"));

        // Parse Date
        if (request.containsKey("eventDate")) {
            event.setEventDate(LocalDateTime.parse(request.get("eventDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (request.containsKey("imageUrl")) {
            event.setImageUrl(request.get("imageUrl"));
        }

        event.setCreatedBy(admin);
        event.setCreatedAt(LocalDateTime.now());

        return eventRepository.save(event);
    }

    // --- UPDATE ---
    public Optional<Event> updateEvent(Long id, Map<String, String> request) {
        return eventRepository.findById(id).map(event -> {
            if (request.containsKey("title")) event.setTitle(request.get("title"));
            if (request.containsKey("description")) event.setDescription(request.get("description"));
            if (request.containsKey("location")) event.setLocation(request.get("location"));

            if (request.containsKey("eventDate")) {
                event.setEventDate(LocalDateTime.parse(request.get("eventDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            if (request.containsKey("imageUrl")) {
                event.setImageUrl(request.get("imageUrl"));
            }

            return eventRepository.save(event);
        });
    }

    // --- DELETE ---
    public boolean deleteEvent(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            return true;
        }
        return false;
    }
}