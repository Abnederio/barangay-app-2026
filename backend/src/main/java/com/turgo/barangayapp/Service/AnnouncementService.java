package com.turgo.barangayapp.Service;

import com.turgo.barangayapp.Model.Announcement;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    // Get All (Public)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findByEventDateAfterOrderByEventDateAsc(LocalDateTime.now());
    }

    // Create (Admin)
    public Announcement createAnnouncement(Map<String, String> request, User admin) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.get("title"));
        announcement.setContent(request.get("content"));

        if (request.containsKey("eventDate") && !request.get("eventDate").isEmpty()) {
            announcement.setEventDate(LocalDateTime.parse(request.get("eventDate")));
        }

        if (request.containsKey("imageUrl")) {
            announcement.setImageUrl(request.get("imageUrl"));
        }
        announcement.setCreatedBy(admin);
        announcement.setCreatedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public Optional<Announcement> updateAnnouncement(Long id, Map<String, String> request) {
        return announcementRepository.findById(id).map(announcement -> {
            announcement.setTitle(request.get("title"));
            announcement.setContent(request.get("content"));

            if (request.containsKey("imageUrl")) {
                announcement.setImageUrl(request.get("imageUrl"));
            }

            return announcementRepository.save(announcement);
        });
    }

    // Delete (Admin)
    public boolean deleteAnnouncement(Long id) {
        if (announcementRepository.existsById(id)) {
            announcementRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Helper to find by ID
    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }
}
